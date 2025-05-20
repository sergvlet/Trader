// src/main/java/com/chicu/trader/trading/context/StrategyContext.java
package com.chicu.trader.trading.context;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import com.chicu.trader.trading.service.CandleService;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Getter
public class StrategyContext {

    private final Long chatId;
    private final String symbol;
    private final Candle candle;
    private final double price;
    private final double atrPct;
    private final double tpPrice;
    private final double slPrice;

    private final CandleService      candleService;
    private final IndicatorService   indicators;
    private final MlSignalFilter     mlFilter;
    private final List<String>       symbols;

    public StrategyContext(
        Long chatId,
        Candle candle,
        List<String> symbols,
        CandleService candleService,
        IndicatorService indicators,
        MlSignalFilter mlFilter
    ) {
        this.chatId        = chatId;
        this.candle        = candle;
        this.symbols       = symbols;
        this.candleService = candleService;
        this.indicators    = indicators;
        this.mlFilter      = mlFilter;

        this.symbol = candle.getSymbol();
        this.price  = candle.getClose();

        // 1) ATR за 14 баров часового интервала
        var hist14 = candleService.history(symbol, Duration.ofHours(1), 14);
        double atrValue = indicators.atr(hist14, 14);
        this.atrPct = atrValue / price;

        // 2) TP/SL на основе ATR%
        double tpPct = Math.max(2.5 * atrPct, 0.03);
        double slPct = Math.max(1.5 * atrPct, 0.01);
        this.tpPrice = price * (1 + tpPct);
        this.slPrice = price * (1 - slPct);
    }

    /** ML-фильтр: true, если модель сигналит BUY */
    public boolean passesMlFilter() {
        var hist = candleService.history(symbol, Duration.ofHours(1), 120);
        double[][] feats = indicators.buildFeatures(hist);
        MarketData md = new MarketData(flatten(feats));
        MarketSignal sig = mlFilter.predict(chatId, md);
        return sig == MarketSignal.BUY;
    }

    /** Объёмная проверка: цена vs VWMA */
    public boolean passesVolume() {
        var hist = candleService.history(symbol, Duration.ofHours(1), 20);
        return candle.getVolume() >= indicators.vwma(hist, 20);
    }

    /** Мульти-таймфрейм: SMA(50) на 4h ниже текущей цены */
    public boolean passesMultiTimeframe() {
        var hist4 = candleService.history(symbol, Duration.ofHours(4), 50);
        return indicators.sma(hist4, 50) < price;
    }

    /** RSI+Bollinger: RSI<32 и цена ниже нижней ленты */
    public boolean passesRsiBb() {
        var hist = candleService.history(symbol, Duration.ofHours(1), 20);
        double rsi = indicators.rsi(hist, 14);
        double lowerBb = indicators.bbLower(hist, 20, 2);
        return rsi < 32 && price < lowerBb && candle.getClose() > candle.getOpen();
    }

    /** Закрытие по верхней полосе BB */
    public boolean shouldCloseByUpperBb() {
        var hist = candleService.history(symbol, Duration.ofHours(1), 20);
        double upperBb = indicators.bbUpper(hist, 20, 2);
        return price > upperBb;
    }

    /** Лог входа */
    public TradeLog toEntryLog() {
        return TradeLog.builder()
            .userChatId(chatId)
            .symbol(symbol)
            .entryTime(Instant.ofEpochMilli(candle.getCloseTime()))
            .entryPrice(price)
            .takeProfitPrice(tpPrice)
            .stopLossPrice(slPrice)
            .isClosed(false)
            .build();
    }

    /** Лог выхода, если достигнут TP/SL или BB */
    public Optional<TradeLog> getExitLog() {
        boolean hitTp = price >= tpPrice;
        boolean hitSl = price <= slPrice;
        boolean hitBb = shouldCloseByUpperBb();
        if (hitTp || hitSl || hitBb) {
            TradeLog exit = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .exitTime(Instant.ofEpochMilli(candle.getCloseTime()))
                .exitPrice(price)
                .pnl(0)      // заполняется в фасаде на основе quantity и entryPrice
                .isClosed(true)
                .build();
            return Optional.of(exit);
        }
        return Optional.empty();
    }

    /** Утилита для ML-фильтра */
    private float[] flatten(double[][] array) {
        int rows = array.length, cols = array[0].length;
        float[] out = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                out[i * cols + j] = (float) array[i][j];
            }
        }
        return out;
    }
}
