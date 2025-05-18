// src/main/java/com/chicu/trader/trading/context/StrategyContext.java
package com.chicu.trader.trading.context;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.BalanceService;
import com.chicu.trader.trading.CandleService;
import com.chicu.trader.trading.PositionService;
import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
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
    private final List<ProfitablePair> pairs;
    private final BalanceService   balanceService;
    private final PositionService  positionService;

    private final double amountUsdt;
    private final double quantity;

    public StrategyContext(Long chatId,
                           Candle candle,
                           List<ProfitablePair> pairs,
                           CandleService candleService,
                           IndicatorService indicators,
                           MlSignalFilter mlFilter,
                           BalanceService balanceService,
                           PositionService positionService) {
        this.chatId          = chatId;
        this.candle          = candle;
        this.pairs           = pairs;
        this.candleService   = candleService;
        this.indicators      = indicators;
        this.mlFilter        = mlFilter;
        this.balanceService  = balanceService;
        this.positionService = positionService;

        this.symbol = candle.getSymbol();
        this.price  = candle.getClose();

        // ATR → TP/SL
        List<Candle> hist14 = candleService.historyHourly(chatId, symbol, 14);
        double atr          = indicators.atr(hist14, 14);
        double tpPct        = Math.max(2.5 * atr / price, 0.03);
        double slPct        = Math.max(1.5 * atr / price, 0.01);
        this.tpPrice        = price * (1 + tpPct);
        this.slPrice        = price * (1 - slPct);

        // рассчитываем объём в USDT и количество актива
        double freeUsdt     = balanceService.getAvailableUsdt(chatId);
        int usedSlots       = positionService.getActiveSlots(chatId);
        int freeSlots       = Math.max(PositionService.MAX_SLOTS - usedSlots, 0);
        this.amountUsdt     = (freeSlots > 0) ? freeUsdt / freeSlots : 0;
        this.quantity       = (price > 0)       ? amountUsdt / price    : 0;
    }

    /** ML-фильтрация по последним 120 часам */
    public boolean passesMlFilter() {
        List<Candle> hist = candleService.historyHourly(chatId, symbol, 120);
        double[][] feats  = indicators.buildFeatures(hist);
        return mlFilter.shouldEnter(feats);
    }

    /** Объём: текущий объём ≥ VWMA(20) */
    public boolean passesVolume() {
        List<Candle> hist = candleService.historyHourly(chatId, symbol, 20);
        return candle.getVolume() >= indicators.vwma(hist, 20);
    }

    /** Мультитаймфрейм 4h SMA(50) < цена */
    public boolean passesMultiTimeframe() {
        List<Candle> hist4 = candleService.history4h(chatId, symbol, 50);
        return indicators.sma(hist4, 50) < price;
    }

    /** RSI(14) <32 и цена < LowerBB(20,2) и свеча бычья */
    public boolean passesRsiBb() {
        List<Candle> hist = candleService.historyHourly(chatId, symbol, 20);
        double rsi        = indicators.rsi(hist, 14);
        double lowerBb    = indicators.bbLower(hist, 20, 2);
        return rsi < 32 && price < lowerBb && candle.getClose() > candle.getOpen();
    }

    /** Закрытие по UpperBB */
    public boolean shouldCloseByUpperBb() {
        List<Candle> hist = candleService.historyHourly(chatId, symbol, 20);
        double upperBb    = indicators.bbUpper(hist, 20, 2);
        return price > upperBb;
    }

    /** Лог входа */
    public com.chicu.trader.model.TradeLog toEntryLog() {
        return com.chicu.trader.model.TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.ofEpochMilli(candle.getCloseTime()))
                .entryPrice(price)
                .takeProfitPrice(tpPrice)
                .stopLossPrice(slPrice)
                .quantity(quantity)
                .isClosed(false)
                .build();
    }

    /** Лог выхода, если позиция закрылась */
    public Optional<com.chicu.trader.model.TradeLog> getExitLog() {
        boolean hitTp = price >= tpPrice;
        boolean hitSl = price <= slPrice;
        boolean hitBb = shouldCloseByUpperBb();
        if (hitTp || hitSl || hitBb) {
            double exitPrice = price;
            double pnl = (exitPrice - price) * quantity;
            com.chicu.trader.model.TradeLog log = com.chicu.trader.model.TradeLog.builder()
                    .userChatId(chatId)
                    .symbol(symbol)
                    .exitTime(Instant.ofEpochMilli(candle.getCloseTime()))
                    .exitPrice(exitPrice)
                    .pnl(pnl)
                    .quantity(quantity)
                    .isClosed(true)
                    .build();
            return Optional.of(log);
        }
        return Optional.empty();
    }
}
