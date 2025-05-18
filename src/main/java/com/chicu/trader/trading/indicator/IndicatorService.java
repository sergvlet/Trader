// src/main/java/com/chicu/trader/trading/indicator/IndicatorService.java
package com.chicu.trader.trading.indicator;

import com.chicu.trader.trading.model.Candle;

import java.util.List;

public interface IndicatorService {
    double rsi(List<Candle> history, int period);
    double bbLower(List<Candle> history, int period, double k);
    double bbUpper(List<Candle> history, int period, double k);
    double sma(List<Candle> history, int period);
    double vwma(List<Candle> history, int period);
    double atr(List<Candle> history, int period);
    double[][] buildFeatures(List<Candle> history);
}
