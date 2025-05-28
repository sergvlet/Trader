package com.chicu.trader.trading.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Интерфейс инференса ML-модели через ONNX.
 */
@Component
@Primary
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class OnnxMlSignalFilter implements MlSignalFilter {

    private final AiTradingSettingsService settingsService;

    @Override
    public MarketSignal predict(Long chatId, MarketData data) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        String path       = String.format(s.getMlModelPath(), chatId);
        String inputName  = s.getMlInputName();
        double threshold  = s.getMlThreshold();

        // если модели нет на диске — быстро выходим
        if (!new File(path).exists()) {
            log.warn("⚠️ ML-модель не найдена: {}", path);
            return MarketSignal.SKIP;
        }

        try {
            // инициализация окружения и сессии
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            try (OrtSession session = env.createSession(path, new OrtSession.SessionOptions())) {

                // подготовка входного тензора
                float[][] inputData = data.toTensorInput();
                OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
                Map<String, OnnxTensor> inputs = Collections.singletonMap(inputName, inputTensor);

                // запуск инференса
                OrtSession.Result output = session.run(inputs);
                float[][] result = (float[][]) output.get(0).getValue();
                float score = result[0][0];

                log.info("🧠 [chatId={}] ML-инференс score = {}", chatId, score);
                return score >= threshold ? MarketSignal.BUY : MarketSignal.SKIP;
            }
        } catch (OrtException | UnsatisfiedLinkError | NoClassDefFoundError e) {
            // если что-то не так с ONNX Runtime, возвращаем безопасный пропуск сигнала
            log.error("❌ ONNX Runtime error, пропускаем ML-фильтр: {}", e.toString());
            return MarketSignal.SKIP;
        } catch (Exception e) {
            // любая другая ошибка — тоже пропускаем
            log.error("❌ Ошибка инференса ML: {}", e.getMessage(), e);
            return MarketSignal.SKIP;
        }
    }
}
