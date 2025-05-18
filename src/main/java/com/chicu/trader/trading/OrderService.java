// src/main/java/com/chicu/trader/trading/OrderService.java
package com.chicu.trader.trading;

import com.binance.connector.client.impl.SpotClientImpl;
import com.chicu.trader.trading.binance.BinanceClientProvider;
import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.model.TradeLogFactory;
import com.chicu.trader.model.TradeLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final BinanceClientProvider clientProvider;

    /**
     * Открывает рыночную позицию и сразу выставляет OCO-ордера TP/SL.
     * Возвращает лог входа.
     */
    public TradeLog openPosition(StrategyContext ctx) {
        SpotClientImpl rest = clientProvider.restClient(ctx.getChatId());

        // 1. Market buy
        LinkedHashMap<String, Object> buyParams = new LinkedHashMap<>();
        buyParams.put("symbol", ctx.getSymbol());
        buyParams.put("side", "BUY");
        buyParams.put("type", "MARKET");
        buyParams.put("quantity", ctx.getQuantity());
        rest.createTrade().newOrder(buyParams);

        // 2. OCO Sell
        LinkedHashMap<String, Object> ocoParams = new LinkedHashMap<>();
        ocoParams.put("symbol", ctx.getSymbol());
        ocoParams.put("side", "SELL");
        ocoParams.put("quantity", ctx.getQuantity());
        ocoParams.put("price", ctx.getTpPrice());
        ocoParams.put("stopPrice", ctx.getSlPrice());
        ocoParams.put("stopLimitPrice", ctx.getSlPrice());
        ocoParams.put("stopLimitTimeInForce", "GTC");
        rest.createTrade().ocoOrder(ocoParams);

        // 3. Формируем лог входа
        return TradeLogFactory.createEntryLog(ctx);
    }

    /**
     * Проверяет условия закрытия и, если нужно, закрывает позицию продажей MARKET.
     * Возвращает Optional<TradeLog> с записью выхода.
     */
    public Optional<TradeLog> checkAndClose(StrategyContext ctx) {
        Optional<TradeLog> exitLogOpt = TradeLogFactory.createExitLog(ctx);
        if (exitLogOpt.isEmpty()) {
            return Optional.empty();
        }

        SpotClientImpl rest = clientProvider.restClient(ctx.getChatId());
        TradeLog exitLog = exitLogOpt.get();

        // Market sell to close
        LinkedHashMap<String, Object> sellParams = new LinkedHashMap<>();
        sellParams.put("symbol", ctx.getSymbol());
        sellParams.put("side", "SELL");
        sellParams.put("type", "MARKET");
        sellParams.put("quantity", ctx.getQuantity());
        rest.createTrade().newOrder(sellParams);

        return Optional.of(exitLog);
    }
}
