package com.chicu.trader.trading.ml;

/**
 * Исключение при ошибке инференса ML-модели.
 */
public class MlInferenceException extends RuntimeException {
    public MlInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
