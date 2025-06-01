// src/main/java/com/chicu/trader/trading/ml/OnnxConfig.java
package com.chicu.trader.trading.ml;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.*;

@Slf4j
@Configuration
public class OnnxConfig {

    /**
     * Пытаемся инициализировать OrtEnvironment. Если native‐DLL не загрузился,
     * ловим UnsatisfiedLinkError и возвращаем null, чтобы бин не создавался.
     */
    @Bean
    @Conditional(OnnxNativeAvailableCondition.class)
    public OrtEnvironment ortEnvironment() {
        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            log.info("✅ ONNX Runtime: OrtEnvironment успешно создан");
            return env;
        } catch (UnsatisfiedLinkError e) {
            log.warn("⚠️ ONNX Runtime недоступен, пропускаем ONNX‐конфигурацию: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Создаём OrtSession только в случае, если OrtEnvironment != null.
     */
    @Bean
    @ConditionalOnBean(OrtEnvironment.class)
    public OrtSession ortSession(OrtEnvironment env,
                                 @Value("${ml.model.path:models/default.onnx}") String modelPath) throws OrtException {
        // Опции по умолчанию
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        log.info("✅ Загружаем ONNX‐модель из: {}", modelPath);
        return env.createSession(modelPath, opts);
    }
}
