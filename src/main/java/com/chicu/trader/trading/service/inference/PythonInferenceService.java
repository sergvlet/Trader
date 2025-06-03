package com.chicu.trader.trading.service.inference;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class PythonInferenceService {

    /**
     * Вызывает Python-скрипт и возвращает вероятность сигнала BUY (от 0.0 до 1.0)
     *
     * @param modelPath путь к обученной модели (например, models/ml_model_rf.pkl)
     * @param features  массив признаков, например [1.0, 0.5, 0.1]
     * @return вероятность BUY от 0.0 до 1.0
     */
    public double predict(String modelPath, double[] features) {
        try {
            String pythonExe = "C:\\Users\\schic\\IdeaProjects\\Trader\\python_ml\\venv\\Scripts\\python.exe";

            Path projectRoot = Paths.get("C:", "Users", "schic", "IdeaProjects", "Trader");
            Path scriptPath  = projectRoot.resolve("python_ml").resolve("predict.py");

            // сериализуем признаки в строку CSV
            String featureInput = serialize(features);

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExe,
                    scriptPath.toString(),
                    "--model", modelPath,
                    "--features", featureInput
            );

            pb.directory(projectRoot.resolve("python_ml").toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[PythonPredict] {}", line);
                    if (line.startsWith("PREDICT=")) {
                        return Double.parseDouble(line.substring(8));
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python скрипт завершился с ошибкой. Код: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("Ошибка при выполнении Python inference", e);
        }

        return 0.5; // по умолчанию: HOLD
    }

    private String serialize(double[] features) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < features.length; i++) {
            sb.append(features[i]);
            if (i < features.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}
