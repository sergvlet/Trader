// src/main/java/com/chicu/trader/trading/ml/DefaultMlModelTrainer.java
package com.chicu.trader.trading.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Заглушечная реализация тренера ML‐модели.
 * Пока что не делает настоящего обучения и всегда возвращает метрики = 0.
 */
@Slf4j
@Component
public class DefaultMlModelTrainer implements MlModelTrainer {

    @Override
    public MlTrainingMetrics trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        long start = System.currentTimeMillis();
        log.info("🔧 (stub) Начало обучения ML-модели для chatId={} → modelPath={}", chatId, modelPath);

        try {
            File out = new File(modelPath);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!out.exists()) {
                out.createNewFile();
            }

            long duration = System.currentTimeMillis() - start;
            log.info("🔧 (stub) Обучение ML-модели завершено за {} ms, модель сохранена по пути {}",
                    duration, modelPath);

            return new MlTrainingMetrics(
                    0.0,         // accuracy
                    0.0,         // auc
                    modelPath,   // куда сохранилась модель
                    duration,    // время тренировки
                    "Stub training completed"
            );
        } catch (Exception e) {
            log.error("❌ Ошибка при stub‐обучении ML‐модели", e);
            throw new MlTrainingException("Stub trainer failed for chatId=" + chatId, e);
        }
    }
}
