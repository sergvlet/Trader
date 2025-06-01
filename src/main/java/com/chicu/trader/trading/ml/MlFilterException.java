// src/main/java/com/chicu/trader/trading/ml/MlFilterException.java
package com.chicu.trader.trading.ml;

/** Простейшее исключение «ошибка ML-фильтра» */
public class MlFilterException extends Exception {
    public MlFilterException(String message, Throwable cause) {
        super(message, cause);
    }
    public MlFilterException(String message) {
        super(message);
    }
}
