package com.chicu.trader.trading.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultMlModelTrainer implements MlModelTrainer {

    @Override
    public MlTrainingMetrics trainAndExport(Long chatId, String modelPath) {
        log.info("🚀 Старт обучения модели для chatId={} и сохранения в {}", chatId, modelPath);

        // Здесь пока просто заглушка, позже подключим реальное обучение
        double accuracy  = 0.85;
        double auc       = 0.90;
        double precision = 0.80;
        double recall    = 0.75;

        return new MlTrainingMetrics(accuracy, auc, precision, recall);
    }
}
