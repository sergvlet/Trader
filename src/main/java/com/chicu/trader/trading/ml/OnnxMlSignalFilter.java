package com.chicu.trader.trading.ml;

import ai.onnxruntime.*;
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

@Slf4j
@Primary
@Order(1)
@Component
@RequiredArgsConstructor
public class OnnxMlSignalFilter implements MlSignalFilter {

    private final AiTradingSettingsService settingsService;

    @Override
    public MarketSignal predict(Long chatId, MarketData data) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);

        String path = String.format(s.getMlModelPath(), chatId);
        String inputName = s.getMlInputName();
        double threshold = s.getMlThreshold();

        if (!new File(path).exists()) {
            log.warn("⚠️ Модель не найдена: {}", path);
            return MarketSignal.BUY;
        }

        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession session = env.createSession(path, new OrtSession.SessionOptions())) {

            float[][] inputData = data.toTensorInput(); // ← теперь работает
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);

            Map<String, OnnxTensor> inputs = Collections.singletonMap(inputName, inputTensor);
            OrtSession.Result output = session.run(inputs);

            float[][] result = (float[][]) output.get(0).getValue();
            float score = result[0][0];

            log.info("🧠 [chatId={}] ML-инференс score = {}", chatId, score);

            return score >= threshold ? MarketSignal.BUY : MarketSignal.SKIP;

        } catch (Exception e) {
            log.error("Ошибка инференса: {}", e.getMessage(), e);
            return MarketSignal.SKIP;
        }
    }
}
