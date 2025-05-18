// src/main/java/com/chicu/trader/trading/ml/MlModelTrainer.java
package com.chicu.trader.trading.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class MlModelTrainer {

    /**
     * Запускает внешний Python-скрипт для тренировки и экспорта ONNX.
     * Ожидает, что скрипт принимает аргумент "--output path".
     */
    public void trainAndExport(String outputOnnxPath) {
        ProcessBuilder pb = new ProcessBuilder(
            "python3", "trainer.py", "--output", outputOnnxPath
        );
        pb.inheritIO();
        try {
            Process p = pb.start();
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("Trainer exited with code " + code);
            }
            log.info("ML model retrained and exported to {}", outputOnnxPath);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run ML trainer", e);
        }
    }
}
