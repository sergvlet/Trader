// src/main/java/com/chicu/trader/trading/model/TradeLogFactory.java
package com.chicu.trader.trading.model;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.context.StrategyContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TradeLogFactory {

    /**
     * Создаёт запись входа по контексту стратегии.
     */
    public TradeLog createEntry(StrategyContext ctx) {
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
     * Создаёт запись выхода по контексту и существующему лог-входу.
     */
    public Optional<TradeLog> createExit(StrategyContext ctx, TradeLog entry) {
        Optional<TradeLog> opt = ctx.getExitLog();
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        TradeLog exit = opt.get();
        // Заполняем поля из entry и выхода
        return Optional.of(
            TradeLog.builder()
                .userChatId(ctx.getChatId())
                .symbol(ctx.getSymbol())
                .entryTime(entry.getEntryTime())
                .entryPrice(entry.getEntryPrice())
                .exitTime(exit.getExitTime())
                .exitPrice(exit.getExitPrice())
                .takeProfitPrice(entry.getTakeProfitPrice())
                .stopLossPrice(entry.getStopLossPrice())
                .pnl(exit.getPnl())
                .isClosed(true)
                .build()
        );
    }
}
