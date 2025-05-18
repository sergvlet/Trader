// src/main/java/com/chicu/trader/trading/model/TradeLogFactory.java
package com.chicu.trader.trading.model;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.context.StrategyContext;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.Optional;

@UtilityClass
public class TradeLogFactory {

    /**
     * Создаёт запись входа по контексту стратегии.
     */
    public TradeLog createEntryLog(StrategyContext ctx) {
        return TradeLog.builder()
            .userChatId(ctx.getChatId())
            .symbol(ctx.getSymbol())
            .entryTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
            .entryPrice(ctx.getPrice())
            .takeProfitPrice(ctx.getTpPrice())
            .stopLossPrice(ctx.getSlPrice())
            .quantity(ctx.getQuantity())
            .isClosed(false)
            .build();
    }

    /**
     * Создаёт запись выхода по контексту стратегии, если позиция закрыта.
     * Возвращает Optional.empty(), если условий выхода не выполнено.
     */
    public Optional<TradeLog> createExitLog(StrategyContext ctx) {
        // Проверяем условия закрытия
        boolean hitTp = ctx.getPrice() >= ctx.getTpPrice();
        boolean hitSl = ctx.getPrice() <= ctx.getSlPrice();
        boolean hitBb = ctx.shouldCloseByUpperBb();
        if (!hitTp && !hitSl && !hitBb) {
            return Optional.empty();
        }

        double exitPrice = ctx.getPrice();
        double pnl = (exitPrice - ctx.getPrice()) * ctx.getQuantity();

        TradeLog log = TradeLog.builder()
            .userChatId(ctx.getChatId())
            .symbol(ctx.getSymbol())
            .exitTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
            .exitPrice(exitPrice)
            .pnl(pnl)
            .quantity(ctx.getQuantity())
            .isClosed(true)
            .build();

        return Optional.of(log);
    }
}
