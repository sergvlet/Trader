package com.chicu.trader.trading.ml;

/**
 * Исключение, возникающее при ошибках тренировки модели.
 */
public class MlTrainingException extends RuntimeException {
    public MlTrainingException(String message, Throwable cause) {
        super(message, cause);
    }
}
