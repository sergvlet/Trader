// src/main/java/com/chicu/trader/trading/context/StrategyContext.java
package com.chicu.trader.trading.context;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.service.CandleService;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Getter
public class StrategyContext {

    private final Long chatId;
    private final String symbol;
    private final Candle candle;
    private final double price;
    private final double tpPrice;
    private final double slPrice;

    private final CandleService    candleService;
    private final IndicatorService indicators;
    private final MlSignalFilter   mlFilter;
    private final List<String>     symbols;

    public StrategyContext(Long chatId,
                           Candle candle,
                           List<String> symbols,
                           CandleService candleService,
                           IndicatorService indicators,
                           MlSignalFilter mlFilter) {
        this.chatId        = chatId;
        this.candle        = candle;
        this.symbols       = symbols;
        this.candleService = candleService;
        this.indicators    = indicators;
        this.mlFilter      = mlFilter;

        this.symbol = candle.getSymbol();
        this.price  = candle.getClose();

        var hist14 = candleService.historyHourly(chatId, symbol, 14);
        double atr   = indicators.atr(hist14, 14);
        double tpPct = Math.max(2.5 * atr / price, 0.03);
        double slPct = Math.max(1.5 * atr / price, 0.01);
        this.tpPrice = price * (1 + tpPct);
        this.slPrice = price * (1 - slPct);
    }

    public boolean passesMlFilter() {
        var hist = candleService.historyHourly(chatId, symbol, 120);
        double[][] feats = indicators.buildFeatures(hist);
        float[] flat     = flatten(feats);
        MarketSignal signal = mlFilter.predict(chatId, new MarketData(flat));
        return signal == MarketSignal.BUY;
    }

    private float[] flatten(double[][] array) {
        int rows = array.length, cols = array[0].length;
        float[] flat = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flat[i * cols + j] = (float) array[i][j];
            }
        }
        return flat;
    }

    public boolean passesVolume() {
        var hist = candleService.historyHourly(chatId, symbol, 20);
        return candle.getVolume() >= indicators.vwma(hist, 20);
    }

    public boolean passesMultiTimeframe() {
        var hist4 = candleService.history4h(chatId, symbol, 50);
        return indicators.sma(hist4, 50) < price;
    }

    public boolean passesRsiBb() {
        var hist = candleService.historyHourly(chatId, symbol, 20);
        double rsi     = indicators.rsi(hist, 14);
        double lowerBb = indicators.bbLower(hist, 20, 2);
        return rsi < 32 && price < lowerBb && candle.getClose() > candle.getOpen();
    }

    public boolean shouldCloseByUpperBb() {
        var hist = candleService.historyHourly(chatId, symbol, 20);
        double upperBb = indicators.bbUpper(hist, 20, 2);
        return price > upperBb;
    }

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

    public Optional<TradeLog> getExitLog() {
        boolean hitTp = price >= tpPrice;
        boolean hitSl = price <= slPrice;
        boolean hitBb = shouldCloseByUpperBb();
        if (hitTp || hitSl || hitBb) {
            double exitPrice = price;
            TradeLog log = TradeLog.builder()
                    .userChatId(chatId)
                    .symbol(symbol)
                    .exitTime(Instant.ofEpochMilli(candle.getCloseTime()))
                    .exitPrice(exitPrice)
                    .pnl(exitPrice - /* entryPrice from db */ 0)
                    .isClosed(true)
                    .build();
            return Optional.of(log);
        }
        return Optional.empty();
    }
}
