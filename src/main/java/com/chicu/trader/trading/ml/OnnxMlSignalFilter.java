// src/main/java/com/chicu/trader/trading/ml/OnnxMlSignalFilter.java
package com.chicu.trader.trading.ml;

import ai.onnxruntime.*;
import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.FloatBuffer;
import java.util.Collections;

@Slf4j
@Component
public class OnnxMlSignalFilter implements MlSignalFilter {

    private final OrtEnvironment env;
    private boolean available;

    public OnnxMlSignalFilter() {
        OrtEnvironment tmp = null;
        boolean ok = false;
        try {
            tmp = OrtEnvironment.getEnvironment();
            ok = true;
            log.info("✅ ONNX Runtime environment initialized");
        } catch (UnsatisfiedLinkError e) {
            log.warn("⚠️ ONNX Runtime not available, ML filter disabled", e);
        }
        this.env = tmp;
        this.available = ok;
    }

    @Override
    public MarketSignal predict(Long chatId, MarketData data) throws MlInferenceException {
        if (!available) {
            // fallback: always BUY
            return MarketSignal.BUY;
        }
        String path = String.format("models/%d/ml_signal_filter.onnx", chatId);
        log.debug("ML inference for chatId={} using model {}", chatId, path);
        try (OrtSession session = env.createSession(path, new OrtSession.SessionOptions())) {
            float[] features = data.toFeatureArray();
            OnnxTensor input = OnnxTensor.createTensor(env,
                    FloatBuffer.wrap(features), new long[]{1, features.length});
            try (OrtSession.Result result = session.run(Collections.singletonMap("input", input))) {
                float[] output = (float[]) result.get(0).getValue();
                float score = output.length > 1 ? output[1] : output[0];
                return score > 0.5f ? MarketSignal.BUY : MarketSignal.SELL;
            }
        } catch (OrtException | UnsatisfiedLinkError e) {
            log.error("❌ Error during ONNX inference for chatId={}", chatId, e);
            throw new MlInferenceException("Inference failed for chatId=" + chatId, e);
        }
    }
}
