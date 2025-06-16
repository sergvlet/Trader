package com.chicu.trader.trading.ml;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrainedModel {
    private final byte[] onnxBytes;    // сериализованный ONNX
    private final double accuracy;
    private final double auc;
    private final double precision;
    private final double recall;
    private final long trainingTimeMillis;

    public void saveToOnnx(String path) throws MlTrainingException {
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(path)) {
            out.write(onnxBytes);
        } catch (Exception e) {
            throw new MlTrainingException("Не удалось сохранить ONNX: " + e.getMessage(), e);
        }
    }
}
