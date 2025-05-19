// src/main/java/com/chicu/trader/trading/ml/DefaultMlModelTrainer.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.repository.TradeLogRepository;
import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final TradeLogRepository logRepo;
    private final CandleService candleService;
    private final IndicatorService   indicatorService;

    @Override
    public void trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        log.info("🔄 Начало тренировки модели для chatId={}", chatId);
        try {
            // 1) Сбор исторических логов по сделкам пользователя
            List<TradeLog> logs = logRepo.findAll().stream()
                    .filter(l -> chatId.equals(l.getUserChatId()))
                    .collect(Collectors.toList());
            if (logs.isEmpty()) {
                log.warn("Нет логов торговли для chatId={}, пропуск тренировки", chatId);
                return;
            }

            // 2) Подготовка фичей и меток
            List<float[]> featureRows = new ArrayList<>(logs.size());
            List<Float>   labels      = new ArrayList<>(logs.size());
            for (TradeLog entry : logs) {
                String symbol = entry.getSymbol();
                // берём последние 100 часов свечей (или другой объём)
                List<Candle> candles = candleService.historyHourly(chatId, symbol, 100);
                double[][] feats      = indicatorService.buildFeatures(candles);
                featureRows.add(flatten(feats));
                // Простая метка: прибыль по сделке > 0
                labels.add(entry.getPnl() > 0 ? 1.0f : 0.0f);
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

    private float[] flatten(double[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        float[] flat = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flat[i * cols + j] = (float) array[i][j];
            }
        }
        return flat;
    }
}
