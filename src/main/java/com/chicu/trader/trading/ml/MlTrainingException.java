package com.chicu.trader.trading.ml;

/**
 * Собственное unchecked‐исключение, которое кидается при ошибках обучения ML‐модели.
 */
public class MlTrainingException extends RuntimeException {
    public MlTrainingException(String message) {
        super(message);
    }

    public MlTrainingException(String message, Throwable cause) {
        super(message, cause);
    }
}
