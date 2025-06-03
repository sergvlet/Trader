package com.chicu.trader.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Сервис запуска Python-обучения через train.py
 */
@Service
@Slf4j
public class MlTrainingService {

    @Value("${ml.python.exe.path:C:\\Users\\schic\\IdeaProjects\\Trader\\python_ml\\venv\\Scripts\\python.exe}")
    private String pythonExe;

    @Value("${ml.data.symbol:ETH/USDT}")
    private String dataSymbol;

    @Value("${ml.data.timeframe:5m}")
    private String dataTimeframe;

    @Value("${ml.data.since.days:30}")
    private int dataSinceDays;

    @Value("${ml.data.limit:500}")
    private int dataLimit;

    // Добавьте эту настройку в application.properties или другое место
    @Value("${ml.data.target.column:target}")
    private String targetColumn;

    /**
     * Запускает скрипт train.py из папки python_ml/data.
     * @return true, если обучение прошло без ошибок (код возврата 0), иначе false.
     */
    public boolean runTraining() {
        try {
            // Текущая директория проекта (полный путь)
            Path projectRoot = Paths.get("").toAbsolutePath();

            // Путь к train.py в папке data
            Path scriptPath = projectRoot.resolve("python_ml").resolve("data").resolve("train.py");

            log.info("Запускаем python: {}", pythonExe);
            log.info("Путь к скрипту train.py: {}", scriptPath.toAbsolutePath());
            log.info("Рабочая директория: {}", scriptPath.getParent().toFile().getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExe,
                    scriptPath.toString()
            );

            // Рабочая директория — папка с train.py (python_ml/data)
            pb.directory(scriptPath.getParent().toFile());

            // Добавляем переменные окружения для передачи параметров скрипту
            Map<String, String> env = pb.environment();
            env.put("DATA_SYMBOL", dataSymbol);
            env.put("DATA_TIMEFRAME", dataTimeframe);

            long sinceMillis = System.currentTimeMillis() - (long) dataSinceDays * 24 * 60 * 60 * 1000;
            env.put("DATA_SINCE", String.valueOf(sinceMillis));
            env.put("DATA_LIMIT", String.valueOf(dataLimit));

            // Передаём имя колонки с целевой меткой
            env.put("ML_TARGET_COLUMN", targetColumn);

            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("✅ Обучение завершилось успешно.");
                return true;
            } else {
                log.error("❌ Обучение завершилось с ошибкой. Код: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("⛔ Ошибка запуска обучения: {}", e.getMessage(), e);
            return false;
        }
    }
}
