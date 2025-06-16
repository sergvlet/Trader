// src/main/java/com/chicu/trader/trading/ml/ModelTrainerInternal.java
package com.chicu.trader.trading.ml;

public interface ModelTrainerInternal {
    /**
     * Тренирует модель на основе Dataset и возвращает объект Model.
     */
    Model train(Dataset dataset) throws MlTrainingException;
}
