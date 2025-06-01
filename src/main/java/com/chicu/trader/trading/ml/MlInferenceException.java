package com.chicu.trader.trading.ml;

/**
 * Собственное unchecked‐исключение, которое кидается при ошибках инференса ML.
 */
public class MlInferenceException extends RuntimeException {
    public MlInferenceException(String message) {
        super(message);
    }

    public MlInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
