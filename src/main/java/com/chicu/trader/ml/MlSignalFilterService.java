package com.chicu.trader.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
@Slf4j
public class MlSignalFilterService {

    private static final double DEFAULT_THRESHOLD = 0.6;

    /**
     * Вызывает predict.py с фичами и получает предсказание.
     */
    public boolean isSignalApproved(Long chatId, List<Double> features) {
        try {
            File tempFile = File.createTempFile("features_" + chatId, ".csv");
            try (PrintWriter pw = new PrintWriter(tempFile)) {
                pw.println(String.join(",", features.stream().map(String::valueOf).toList()));
            }

            String modelPath = String.format("python_ml/data/models/rf_model.pkl");
            ProcessBuilder pb = new ProcessBuilder("python", "python_ml/data/predict.py",
                    "--model", modelPath,
                    "--features", tempFile.getAbsolutePath());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[ML] Python output: {}", line);
                    if (line.startsWith("PREDICT=")) {
                        double prob = Double.parseDouble(line.substring("PREDICT=".length()));
                        return prob >= DEFAULT_THRESHOLD;
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            log.error("Ошибка при вызове Python модели:", e);
        }

        return false;
    }
}
