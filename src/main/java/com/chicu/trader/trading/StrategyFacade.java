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

    /**
     * Основной метод — вызывается на каждом новом закрытом баре.
     */
    public void applyStrategies(Long chatId, Candle currentCandle, List<ProfitablePair> pairs) {
        // 1) Собираем список символов
        List<String> symbols = pairs.stream()
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.toList());

        // 2) Строим контекст стратегии
        StrategyContext ctx = new StrategyContext(
                chatId,
                currentCandle,
                symbols,
                candleService,
                indicatorService,
                mlFilter
        );

        // 3) Вход: если все фильтры прошли
        if (ctx.passesMlFilter()
                && ctx.passesVolume()
                && ctx.passesMultiTimeframe()
                && ctx.passesRsiBb()) {
            enterTrade(ctx);
        }

        // 4) Выход: закрываем все открытые сделки по паре, если сработал TP или SL
        ctx.getExitLog().ifPresent(this::exitAllTrades);
    }

    /**
     * Открытие позиции через OCO-ордер (TP+SL).
     * Объём = riskThreshold% от свободного баланса.
     */
    private void enterTrade(StrategyContext ctx) {
        Long chatId = ctx.getChatId();
        String symbol = ctx.getSymbol();

        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        double riskPct = settings.getRiskThreshold() != null ? settings.getRiskThreshold() : 0.0;

        double price   = ctx.getPrice();
        // Берём свободный баланс в базовой валюте (например, USD)
        double balance = accountService.getFreeBalance(
                chatId,
                symbol.replaceAll("[A-Z]+$", "")
        );

        double usd  = balance * riskPct / 100.0;
        double qty  = usd > 0 ? usd / price : 0.0;

        // Ставим OCO: SL и TP из контекста
        orderService.placeOcoOrder(
                chatId,
                symbol,
                qty,
                ctx.getSlPrice(),
                ctx.getTpPrice()
        );

        // Сохраняем вход в TradeLog
        TradeLog entry = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.ofEpochMilli(ctx.getCandle().getCloseTime()))
                .entryPrice(price)
                .takeProfitPrice(ctx.getTpPrice())
                .stopLossPrice(ctx.getSlPrice())
                .quantity(qty)
                .isClosed(false)
                .build();
        tradeLogRepository.save(entry);

        log.info("Entered trade: chatId={}, symbol={}, qty={}, TP={}, SL={}",
                chatId, symbol, qty, ctx.getTpPrice(), ctx.getSlPrice());
    }

    /**
     * Закрытие всех незакрытых сделок по символу,
     * если цена достигла TP или SL.
     *
     * Теперь принимает StrategyContext.ExitLog вместо TradeLog.
     */
    private void exitAllTrades(StrategyContext.ExitLog exitInfo) {
        Long chatId   = exitInfo.getChatId();
        String symbol = exitInfo.getSymbol();
        double exitPr = exitInfo.getExitPrice();

        // Находим все открытые сделки (isClosed = false) по данному символу
        List<TradeLog> openTrades = tradeLogRepository
                .findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);

        for (TradeLog open : openTrades) {
            double entryPr = open.getEntryPrice();
            double qty     = open.getQuantity();
            Double tp      = open.getTakeProfitPrice();
            Double sl      = open.getStopLossPrice();

            boolean hitTp = tp != null && exitPr >= tp;
            boolean hitSl = sl != null && exitPr <= sl;
            if (!hitTp && !hitSl) {
                log.debug("Skip close for chatId={}, symbol={} at {}: no TP({})/SL({}) hit",
                        chatId, symbol, exitPr, tp, sl);
                continue;
            }

            // Рыночный ордер на закрытие всей позиции
            orderService.placeMarketOrder(chatId, symbol, qty);

            open.setExitTime(Instant.now());
            open.setClosed(true);
            open.setExitPrice(exitPr);
            open.setPnl((exitPr - entryPr) * qty);
            tradeLogRepository.save(open);

            log.info("Exited trade: chatId={}, symbol={}, qty={}, exitPrice={}, PnL={}, reason={}",
                    chatId, symbol, qty, exitPr, open.getPnl(),
                    hitTp ? "TP" : "SL");
        }
    }
}
