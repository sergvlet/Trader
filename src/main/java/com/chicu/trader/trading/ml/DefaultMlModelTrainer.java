package com.chicu.trader.trading.ml;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final ProfitablePairRepository pairRepo;
    private final TradeLogRepository logRepo;
    private final CandleService candleService;
    private final IndicatorService indicatorService;

    @Override
    public MlTrainingMetrics trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        log.info("🔄 Начало тренировки модели для chatId={}", chatId);
        try {
            List<TradeLog> logs = logRepo.findAllByUserChatId(chatId);
            log.info("📄 TradeLog записей: {}", logs.size());

            if (logs.isEmpty()) {
                log.warn("❌ Нет логов торговли для chatId={}, пропуск тренировки", chatId);
                return emptyMetrics();
            }

            List<float[]> featureRows = new ArrayList<>();
            List<Float> labels = new ArrayList<>();
            int skipped = 0;

            for (TradeLog entry : logs) {
                String symbol = entry.getSymbol();
                List<Candle> candles = candleService.history(symbol, Duration.ofHours(1), 120);
                double[][] feats = indicatorService.buildFeatures(candles);
                if (feats == null || feats.length == 0) {
                    log.warn("⛔ Пропуск symbol={} — недостаточно данных", symbol);
                    skipped++;
                    continue;
                }

                float[] features = new float[feats.length * feats[0].length];
                for (int i = 0; i < feats.length; i++) {
                    for (int j = 0; j < feats[i].length; j++) {
                        features[i * feats[i].length + j] = (float) feats[i][j];
                    }
                }
                featureRows.add(features);
                labels.add(entry.getPnl() > 0 ? 1.0f : 0.0f);
            }

            if (featureRows.isEmpty()) {
                log.warn("❌ Ни одной сделки не прошло фильтрацию. Пропуск обучения.");
                return emptyMetrics();
            }

            int rows = featureRows.size();
            int cols = featureRows.get(0).length;
            float[] data = new float[rows * cols];
            float[] labelArr = new float[rows];
            for (int i = 0; i < rows; i++) {
                System.arraycopy(featureRows.get(i), 0, data, i * cols, cols);
                labelArr[i] = labels.get(i);
            }

            DMatrix trainMat = new DMatrix(data, rows, cols, Float.NaN);
            trainMat.setLabel(labelArr);

            Map<String, Object> params = new HashMap<>();
            params.put("objective", "binary:logistic");
            params.put("eval_metric", "logloss");
            params.put("max_depth", 6);
            params.put("eta", 0.1);

            Booster booster = XGBoost.train(trainMat, params, 100, new HashMap<>(), null, null);

            // ✅ создать директорию при необходимости
            File file = new File(modelPath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    log.info("📂 Создана директория модели: {}", parentDir.getAbsolutePath());
                } else {
                    log.warn("⚠️ Не удалось создать директорию: {}", parentDir.getAbsolutePath());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                booster.saveModel(fos);
            }

            log.info("✅ Модель сохранена: chatId={} → {}", chatId, modelPath);

            float[][] raw = booster.predict(trainMat);
            float[] preds = new float[raw.length];
            for (int i = 0; i < raw.length; i++) {
                preds[i] = raw[i][0];
            }

            int tp = 0, tn = 0, fp = 0, fn = 0;
            for (int i = 0; i < preds.length; i++) {
                float actual = labelArr[i];
                float predicted = preds[i] >= 0.5f ? 1.0f : 0.0f;
                if (actual == 1.0f && predicted == 1.0f) tp++;
                if (actual == 0.0f && predicted == 0.0f) tn++;
                if (actual == 0.0f && predicted == 1.0f) fp++;
                if (actual == 1.0f && predicted == 0.0f) fn++;
            }

            int total = tp + tn + fp + fn;
            double accuracy = total > 0 ? (tp + tn) / (double) total : 0.0;
            double precision = (tp + fp) > 0 ? tp / (double) (tp + fp) : 0.0;
            double recall = (tp + fn) > 0 ? tp / (double) (tp + fn) : 0.0;
            double auc = estimateAuc(labelArr, preds);

            log.info("📊 Метрики: acc={}", String.format("%.4f", accuracy));
            log.info("📊           pr={} rec={} auc={}",
                    String.format("%.4f", precision),
                    String.format("%.4f", recall),
                    String.format("%.4f", auc)
            );
            log.info("📊 Пропущено сделок: {}", skipped);

            return MlTrainingMetrics.builder()
                    .accuracy(accuracy)
                    .precision(precision)
                    .recall(recall)
                    .auc(auc)
                    .build();

        } catch (Exception e) {
            log.error("❌ Ошибка обучения модели для chatId=" + chatId, e);
            throw new MlTrainingException("Ошибка тренировки модели", e);
        }
    }

    private MlTrainingMetrics emptyMetrics() {
        return MlTrainingMetrics.builder()
                .accuracy(0)
                .precision(0)
                .recall(0)
                .auc(0)
                .build();
    }

    private double estimateAuc(float[] labels, float[] scores) {
        List<Map.Entry<Float, Float>> pairs = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            pairs.add(Map.entry(scores[i], labels[i]));
        }
        pairs.sort((a, b) -> -Float.compare(a.getKey(), b.getKey()));

        int pos = 0, neg = 0;
        for (float l : labels) {
            if (l == 1.0f) pos++;
            else neg++;
        }

        int tp = 0, fp = 0;
        double prevScore = Double.NaN;
        double auc = 0.0;
        int prevTp = 0, prevFp = 0;

        for (Map.Entry<Float, Float> pair : pairs) {
            float score = pair.getKey();
            float label = pair.getValue();
            if (score != prevScore) {
                auc += trapezoidArea(fp, tp, prevFp, prevTp);
                prevScore = score;
                prevTp = tp;
                prevFp = fp;
            }
            if (label == 1.0f) tp++;
            else fp++;
        }

        auc += trapezoidArea(fp, tp, prevFp, prevTp);
        return (pos > 0 && neg > 0) ? auc / (pos * neg) : 0.0;
    }

    private double trapezoidArea(int x1, int y1, int x0, int y0) {
        return 0.5 * (x1 - x0) * (y1 + y0);
    }
}
