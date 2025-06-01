// src/main/java/com/chicu/trader/trading/ml/OnnxNativeAvailableCondition.java
package com.chicu.trader.trading.ml;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Условие, которое позволяет зарегистрировать ONNX-бины только в случае
 * успешной инициализации OrtEnvironment (то есть нативная библиотека загружается).
 */
public class OnnxNativeAvailableCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            // Пытаемся инициализировать OrtEnvironment
            OrtEnvironment.getEnvironment();
            return true;
        } catch (UnsatisfiedLinkError e) {
            // Если не удалось загрузить нативный код ONNX Runtime, возвращаем false
            return false;
        }
    }
}
