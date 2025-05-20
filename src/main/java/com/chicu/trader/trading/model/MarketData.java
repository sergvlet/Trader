package com.chicu.trader.trading.model;

/**
 * Контейнер для входных фич ML-модели.
 */
public class MarketData {

    private final float[] features;

    public MarketData(float[] features) {
        this.features = features;
    }

    /**
     * Возвращает массив фичей для передачи в ONNX.
     */
    public float[] toFeatureArray() {
        return features;
    }

    /**
     * Возвращает двумерный массив для ONNX (batch = 1).
     */
    public float[][] toTensorInput() {
        return new float[][] { features };
    }
}
