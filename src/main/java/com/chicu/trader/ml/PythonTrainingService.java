package com.chicu.trader.ml;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PythonTrainingService {
    /**
     * Запускает скрипт train.py из папки python_ml.
     * @return true, если обучение прошло без ошибок (код возврата 0), иначе false.
     */
    public boolean runTraining() {
        try {
            // 1) Абсолютный путь до python.exe в вашем venv:
            String pythonExe = "C:\\Users\\schic\\IdeaProjects\\Trader\\python_ml\\venv\\Scripts\\python.exe";

            // 2) Абсолютный путь до train.py
            Path projectRoot = Paths.get("C:", "Users", "schic", "IdeaProjects", "Trader");
            Path scriptPath  = projectRoot.resolve("python_ml").resolve("train.py");

            // 3) Формируем команду
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExe,
                    scriptPath.toString()
            );

            // Указываем рабочую директорию (папка python_ml), чтобы внутри скрипта относительные пути работали корректно
            pb.directory(projectRoot.resolve("python_ml").toFile());
            // Объединяем stderr и stdout
            pb.redirectErrorStream(true);

            // 4) Запускаем процесс
            Process process = pb.start();

            // 5) Читаем вывод скрипта в режиме реального времени и логируем его
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Можно заменить на log.info(), если нужен логгер Spring
                    System.out.println("[Python] " + line);
                }
            }

            // 6) Ждём завершения процесса и проверяем код возврата
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Обучение завершилось успешно.");
                return true;
            } else {
                System.err.println("Обучение завершилось с ошибкой. Код: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
