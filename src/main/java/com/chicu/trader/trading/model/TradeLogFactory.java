// src/main/java/com/chicu/trader/trading/model/TradeLogFactory.java
package com.chicu.trader.trading.model;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.context.StrategyContext;
import java.time.Instant;

public class TradeLogFactory {

    private TradeLogFactory() {
        // Невозможно инстанцировать
    }

    /**
     * Построить лог входа по контексту стратегии.
     */
    public static TradeLog createEntryLog(StrategyContext ctx) {
        return TradeLog.builder()
                .userChatId(ctx.getChatId())
                .symbol(ctx.getSymbol())
                .entryTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
                .entryPrice(ctx.getPrice())
                .takeProfitPrice(ctx.getTpPrice())
                .stopLossPrice(ctx.getSlPrice())
                .isClosed(false)
                .build();
    }

    /**
     * Построить лог выхода по сохранённому логу входа и текущему контексту.
     * Берёт exitPrice и exitTime из контекста, расчёт PnL делает по entryPrice из переданного log.
     */
    public static TradeLog createExitLog(StrategyContext ctx, TradeLog entryLog) {
        double exitPrice = ctx.getPrice();
        double pnl       = (exitPrice - entryLog.getEntryPrice()) * entryLog.getQuantity();

        return TradeLog.builder()
                .userChatId(ctx.getChatId())
                .symbol(ctx.getSymbol())
                .entryTime(entryLog.getEntryTime())
                .entryPrice(entryLog.getEntryPrice())
                .takeProfitPrice(entryLog.getTakeProfitPrice())
                .stopLossPrice(entryLog.getStopLossPrice())
                .exitTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
                .exitPrice(exitPrice)
                .pnl(pnl)
                .isClosed(true)
                .build();
    }
}
