// src/main/java/com/chicu/trader/trading/model/TradeLogFactory.java
package com.chicu.trader.trading.model;

import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.entity.TradeLog;

import java.math.BigDecimal;
import java.time.Instant;

public class TradeLogFactory {

    private TradeLogFactory() {
        // утилитарный класс — запрещаем инстанцировать
    }

    /**
     * Лог входа: из контекста берём цену (BigDecimal), время, TP/SL и количество (BigDecimal).
     */
    public static TradeLog createEntryLog(StrategyContext ctx) {
        BigDecimal price = ctx.getPrice();
        BigDecimal qty   = ctx.getOrderQuantity();

        return TradeLog.builder()
                .userChatId(ctx.getChatId())
                .symbol(ctx.getSymbol())
                .entryTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
                // Теперь entryPrice и quantity — BigDecimal
                .entryPrice(price)
                .quantity(qty)
                .takeProfitPrice(ctx.getTpPrice())
                .stopLossPrice(ctx.getSlPrice())
                .isClosed(false)
                .build();
    }

    /**
     * Лог выхода: берём из entryLog BigDecimal-поля, добавляем exit-поля и считаем PnL.
     */
    public static TradeLog createExitLog(StrategyContext ctx, TradeLog entryLog) {
        BigDecimal exitPrice = ctx.getPrice();
        BigDecimal entryPrice = entryLog.getEntryPrice();
        BigDecimal quantity   = entryLog.getQuantity();

        // PnL = (exitPrice – entryPrice) * quantity
        BigDecimal pnl = exitPrice
                .subtract(entryPrice)
                .multiply(quantity);

        return TradeLog.builder()
                .userChatId(ctx.getChatId())
                .symbol(ctx.getSymbol())
                .entryTime(entryLog.getEntryTime())
                .entryPrice(entryPrice)
                .quantity(quantity)
                .takeProfitPrice(entryLog.getTakeProfitPrice())
                .stopLossPrice(entryLog.getStopLossPrice())
                .exitTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
                .exitPrice(exitPrice)
                .pnl(pnl)
                .isClosed(true)
                .build();
    }
}
