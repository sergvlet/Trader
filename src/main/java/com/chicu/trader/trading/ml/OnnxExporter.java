package com.chicu.trader.trading.ml;

import java.io.FileOutputStream;
import java.io.IOException;

public class OnnxExporter {
    public static void export(Model model, String path) throws MlTrainingException {
        // TODO: здесь ваша логика сериализации в ONNX
        // Для заглушки просто создадим файл:
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(("Model weights: " + java.util.Arrays.toString(model.getWeights())).getBytes());
        } catch (IOException e) {
            throw new MlTrainingException("Ошибка экспорта модели в ONNX", e);
        }
    }
}
