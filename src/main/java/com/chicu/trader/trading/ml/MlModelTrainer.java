package com.chicu.trader.trading.ml;

/**
 * Интерфейс тренировки ML-модели.
 */
public interface MlModelTrainer {
    /**
     * Обучить модель для chatId и сохранить артефакт по пути modelPath.
     */
    void trainAndExport(Long chatId, String modelPath) throws MlTrainingException;
}
