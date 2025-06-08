// src/main/java/com/chicu/trader/trading/service/binance/BinanceOrderException.java
package com.chicu.trader.trading.service.binance;

/**
 * Выбрасывается при ошибках отправки ордеров на Binance
 */
public class BinanceOrderException extends Exception {

    public BinanceOrderException(String message) {
        super(message);
    }

    public BinanceOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
