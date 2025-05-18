// src/main/java/com/chicu/trader/trading/ml/MlSignalFilter.java
package com.chicu.trader.trading.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class MlSignalFilter {

    private OrtEnvironment env;
    private OrtSession session;

    /** Загружает ONNX-модель из файла. */
    public void loadModel(String modelPath) {
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(modelPath, new OrtSession.SessionOptions());
            log.info("ONNX model loaded from {}", modelPath);
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load ONNX model", e);
        }
    }

    /**
     * Запускает инференс на массиве features (размер [n][m]).
     * Возвращает true, если выход > 0.5.
     */
    public boolean shouldEnter(double[][] features) {
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, features)) {
            OrtSession.Result results = session.run(Map.of("input", tensor));
            float[][] output = (float[][]) results.get(0).getValue();
            return output.length > 0 && output[0].length > 0 && output[0][0] > 0.5f;
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed", e);
        }
    }
}
