// src/main/java/com/chicu/trader/trading/StrategyFacade.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.AccountService;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.OrderService;
import com.chicu.trader.trading.indicator.IndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StrategyFacade {

    private final CandleService            candleService;
    private final IndicatorService         indicatorService;
    private final MlSignalFilter           mlFilter;
    private final AiTradingSettingsService settingsService;
    private final AccountService           accountService;
    private final OrderService             orderService;
    private final TradeLogRepository       tradeLogRepository;

    public void applyStrategies(Long chatId, Candle currentCandle, List<ProfitablePair> pairs) {
        List<String> symbols = pairs.stream()
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.toList());

        StrategyContext ctx = new StrategyContext(
                chatId,
                currentCandle,
                symbols,
                candleService,
                indicatorService,
                mlFilter
        );

        if (ctx.passesMlFilter()
                && ctx.passesVolume()
                && ctx.passesMultiTimeframe()
                && ctx.passesRsiBb()) {
            enterTrade(ctx);
        }

        ctx.getExitLog().ifPresent(this::exitTrade);
    }

    private void enterTrade(StrategyContext ctx) {
        Long chatId = ctx.getChatId();
        String symbol = ctx.getSymbol();
        AiTradingSettings settings = settingsService.getOrCreate(chatId);

        double balance = accountService.getFreeBalance(chatId, symbol.replaceAll("[A-Z]+$", ""));
        double riskPct = settings.getRiskThreshold();
        double usd     = balance * riskPct / 100.0;
        double qty     = usd / ctx.getPrice();

        orderService.placeOcoOrder(
                chatId,
                symbol,
                qty,
                ctx.getSlPrice(),
                ctx.getTpPrice()
        );

        TradeLog entry = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
                .entryPrice(ctx.getPrice())
                .takeProfitPrice(ctx.getTpPrice())
                .stopLossPrice(ctx.getSlPrice())
                .quantity(qty)
                .isClosed(false)
                .build();
        tradeLogRepository.save(entry);

        log.info("Вход: chatId={}, symbol={}, qty={}, TP={}, SL={}",
                chatId, symbol, qty, ctx.getTpPrice(), ctx.getSlPrice());
    }

    private void exitTrade(TradeLog logEntry) {
        Long chatId    = logEntry.getUserChatId();
        String symbol  = logEntry.getSymbol();
        double qty     = logEntry.getQuantity();
        double exitPr  = logEntry.getExitPrice();

        orderService.placeMarketOrder(chatId, symbol, qty);

        logEntry.setExitTime(Instant.now());
        logEntry.setClosed(true);
        logEntry.setPnl((exitPr - logEntry.getEntryPrice()) * qty);
        tradeLogRepository.save(logEntry);

        log.info("Выход: chatId={}, symbol={}, qty={}, exitPrice={}, PnL={}",
                chatId, symbol, qty, exitPr, logEntry.getPnl());
    }
}
