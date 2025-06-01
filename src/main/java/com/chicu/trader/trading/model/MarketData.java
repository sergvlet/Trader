// src/main/java/com/chicu/trader/trading/model/MarketData.java
package com.chicu.trader.trading.model;

import lombok.Getter;

/**
 * Обёртка над сырыми фичами для ML.
 */
@Getter
public class MarketData {
    private final float[] features;
    public MarketData(float[] features) {
        this.features = features;
    }
}
