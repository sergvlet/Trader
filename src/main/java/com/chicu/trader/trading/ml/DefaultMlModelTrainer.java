// src/main/java/com/chicu/trader/trading/ml/DefaultMlModelTrainer.java
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

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final ProfitablePairRepository pairRepo;
    private final TradeLogRepository logRepo;
    private final CandleService            candleService;
    private final IndicatorService         indicatorService;

    @Override
    public void trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        log.info("🔄 Начало тренировки модели для chatId={}", chatId);
        try {
            // 1) Сбор исторических логов по сделкам пользователя
            List<TradeLog> logs = logRepo.findAllByUserChatId(chatId);
            if (logs.isEmpty()) {
                log.warn("Нет логов торговли для chatId={}, пропуск тренировки", chatId);
                return;
            }

            // 2) Подготовка фичей и меток
            List<float[]> featureRows = new ArrayList<>(logs.size());
            List<Float> labels        = new ArrayList<>(logs.size());
            for (TradeLog entry : logs) {
                String symbol = entry.getSymbol();
                List<Candle> candles = candleService.history(symbol, Duration.ofHours(1), 120);
                // используем buildFeatures вместо calculateFeatures
                double[][] feats = indicatorService.buildFeatures(candles);
                float[] features = new float[feats.length * feats[0].length];
                for (int i = 0; i < feats.length; i++) {
                    for (int j = 0; j < feats[i].length; j++) {
                        features[i * feats[i].length + j] = (float) feats[i][j];
                    }
                }
                featureRows.add(features);
                // заменяем getProfit() на getPnl()
                float label = entry.getPnl() > 0 ? 1.0f : 0.0f;
                labels.add(label);
            }

            // 3) Сбор в один массив
            int rows = featureRows.size();
            int cols = featureRows.get(0).length;
            float[] data     = new float[rows * cols];
            float[] labelArr = new float[rows];
            for (int i = 0; i < rows; i++) {
                System.arraycopy(featureRows.get(i), 0, data, i * cols, cols);
                labelArr[i] = labels.get(i);
            }

            // 4) Создание DMatrix для XGBoost
            DMatrix trainMat = new DMatrix(data, rows, cols, Float.NaN);
            trainMat.setLabel(labelArr);

            // 5) Параметры обучения
            Map<String, Object> params = new HashMap<>();
            params.put("objective", "binary:logistic");
            params.put("eval_metric", "logloss");
            params.put("max_depth", 6);
            params.put("eta", 0.1);

            // 6) Тренировка модели
            Booster booster = XGBoost.train(trainMat, params, 100, new HashMap<>(), null, null);

            // 7) Сохранение модели
            try (FileOutputStream fos = new FileOutputStream(modelPath)) {
                booster.saveModel(fos);
            }
            log.info("✅ Модель для chatId={} успешно сохранена в {}", chatId, modelPath);

        } catch (Exception e) {
            log.error("❌ Ошибка тренировки модели для chatId={}", chatId, e);
            throw new MlTrainingException("Ошибка тренировки модели для " + chatId, e);
        }
    }
}
