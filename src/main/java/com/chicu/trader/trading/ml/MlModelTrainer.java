// src/main/java/com/chicu/trader/trading/ml/MlModelTrainer.java
package com.chicu.trader.trading.ml;

/**
 * Интерфейс для тренировки ML-модели и экспорта в ONNX.
 */
public interface MlModelTrainer {
    /**
     * Обучить модель и записать её в файл onnxPath.
     */
    void trainAndExport(String onnxPath);
}
