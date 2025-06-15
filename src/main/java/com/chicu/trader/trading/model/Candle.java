// src/main/java/com/chicu/trader/trading/model/Candle.java
package com.chicu.trader.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Данные одной свечи (клина) с биржи.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    /** Торговая пара, например "BTCUSDT" */
    private String symbol;
    /** Время открытия свечи в миллисекундах Unix epoch */
    private long   openTime;
    /** Цена открытия */
    private double open;
    /** Максимальная цена */
    private double high;
    /** Минимальная цена */
    private double low;
    /** Цена закрытия */
    private double close;
    /** Объем */
    private double volume;
    /** Время закрытия свечи в миллисекундах Unix epoch */
    private long   closeTime;
}
