package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.model.TradeLog;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.AccountService;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StrategyFacade {

    private final StrategyRegistry           registry;
    private final AiTradingSettingsService   settingsService;
    private final CandleService              candleService;
    private final AccountService             accountService;
    private final OrderService               orderService;
    private final TradeLogRepository         tradeLogRepository;

    public void applyStrategies(Long chatId, List<ProfitablePair> pairs) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        TradeStrategy strat = registry.getByType(settings.getStrategy());
        log.info("StrategyFacade: для chatId={} выбрана стратегия {}", chatId, settings.getStrategy());

        for (ProfitablePair pair : pairs) {
            String symbol = pair.getSymbol();
            log.info("StrategyFacade: начинаем обработку пары symbol={} для chatId={}", symbol, chatId);

            Duration interval = parseDuration(settings.getTimeframe());
            List<Candle> candles = candleService.history(symbol, interval, settings.getCachedCandlesLimit());
            if (candles == null || candles.isEmpty()) {
                log.warn("StrategyFacade: нет свечей для symbol={} chatId={}", symbol, chatId);
                continue;
            }

            log.debug("StrategyFacade: получено {} свечей для symbol={} chatId={}", candles.size(), symbol, chatId);

            double lastClose = candles.get(candles.size() - 1).getClose();
            SignalType signal = strat.evaluate(candles, settings);
            log.info("StrategyFacade: сигнал стратегии для symbol={} chatId={} -> {}", symbol, chatId, signal);

            double currentPrice = lastClose;

            switch (signal) {
                case BUY -> {
                    log.info("StrategyFacade: найден сигнал BUY для symbol={} chatId={} (price={})", symbol, chatId, currentPrice);
                    enterTrade(chatId, symbol, currentPrice, settings, pair);
                }
                case SELL -> {
                    log.info("StrategyFacade: найден сигнал SELL для symbol={} chatId={} (price={})", symbol, chatId, currentPrice);
                    exitAllTrades(chatId, symbol, currentPrice);
                }
                case HOLD -> {
                    log.debug("StrategyFacade: сигнал HOLD для symbol={} chatId={} (price={})", symbol, chatId, currentPrice);
                }
            }
        }
    }

    private void enterTrade(Long chatId,
                            String symbol,
                            double price,
                            AiTradingSettings settings,
                            ProfitablePair pair) {

        double riskPct = settings.getRiskThreshold() != null ? settings.getRiskThreshold() : 0.0;
        log.debug("enterTrade: chatId={}, symbol={}, riskPct={}", chatId, symbol, riskPct);

        String baseAsset = extractBaseAsset(symbol);
        if (baseAsset.isEmpty()) {
            log.error("enterTrade: не удалось определить baseAsset для symbol={} chatId={}", symbol, chatId);
            return;
        }

        double balance = accountService.getFreeBalance(chatId, baseAsset);
        log.info("enterTrade: свободный баланс для chatId={} asset={} = {}", chatId, baseAsset, balance);

        double usdValue = balance * riskPct / 100.0;
        double qty = usdValue > 0 ? roundQuantity(usdValue / price) : 0.0;

        // GTI = округлённое qty
        log.info("enterTrade: рассчитанное qty (GTI) для chatId={}, symbol={} = {}", chatId, symbol, qty);

        if (qty <= 0) {
            log.warn("enterTrade: qty=0 для chatId={}, symbol={}, balance={}, riskPct={}",
                    chatId, symbol, balance, riskPct);
            return;
        }

        double tpPct = pair.getTakeProfitPct();
        double slPct = pair.getStopLossPct();

        double tpPrice = (tpPct > 0)
                ? price * (1 + tpPct / 100.0)
                : price * (1 + riskPct / 100.0);
        double slPrice = (slPct > 0)
                ? price * (1 - slPct / 100.0)
                : price * (1 - riskPct / 100.0);

        log.info("enterTrade: рассчитываем TP={} SL={} для symbol={} chatId={}", tpPrice, slPrice, symbol, chatId);

        orderService.placeOcoOrder(chatId, symbol, qty, slPrice, tpPrice);
        log.info("enterTrade: отправлен OCO-ордер chatId={}, symbol={}, qty={}, SL={}, TP={}",
                chatId, symbol, qty, slPrice, tpPrice);

        TradeLog entry = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(price)
                .takeProfitPrice(tpPrice)
                .stopLossPrice(slPrice)
                .quantity(qty)
                .isClosed(false)
                .build();
        tradeLogRepository.save(entry);

        log.info("Entered trade: chatId={}, symbol={}, qty={}, entryPrice={}, TP={}, SL={}",
                chatId, symbol, qty, price, tpPrice, slPrice);
    }

    private void exitAllTrades(Long chatId, String symbol, double exitPrice) {
        log.debug("exitAllTrades: chatId={}, symbol={}, exitPrice={}", chatId, symbol, exitPrice);

        List<TradeLog> openTrades = tradeLogRepository
                .findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        log.debug("exitAllTrades: найдено {} открытых сделок для symbol={} chatId={}", openTrades.size(), symbol, chatId);

        for (TradeLog open : openTrades) {
            double entryPrice = open.getEntryPrice();
            double qty = open.getQuantity();
            double tp = open.getTakeProfitPrice();
            double sl = open.getStopLossPrice();

            boolean hitTp = (tp > 0) && (exitPrice >= tp);
            boolean hitSl = (sl > 0) && (exitPrice <= sl);
            if (!hitTp && !hitSl) {
                log.info("exitAllTrades: force-close (SELL-сигнал) для chatId={}, symbol={}", chatId, symbol);
            }

            orderService.placeMarketOrder(chatId, symbol, qty);
            log.info("exitAllTrades: отправлен рыночный ордер на закрытие chatId={}, symbol={}, qty={}", chatId, symbol, qty);

            open.setExitTime(Instant.now());
            open.setClosed(true);
            open.setExitPrice(exitPrice);
            open.setPnl((exitPrice - entryPrice) * qty);
            tradeLogRepository.save(open);

            String reason = hitTp ? "TP" : (hitSl ? "SL" : "SELL_SIGNAL");
            log.info("Exited trade: chatId={}, symbol={}, qty={}, exitPrice={}, PnL={}, reason={}",
                    chatId, symbol, qty, open.getExitPrice(), open.getPnl(), reason);
        }
    }

    private String extractBaseAsset(String symbol) {
        String[] quoteAssets = {"USDT", "BUSD", "BTC", "ETH"};
        for (String quote : quoteAssets) {
            if (symbol.endsWith(quote)) {
                return symbol.substring(0, symbol.length() - quote.length());
            }
        }
        return "";
    }

    private double roundQuantity(double qty) {
        return Math.floor(qty * 1000) / 1000.0; // округляем до 3 знаков после запятой
    }

    private Duration parseDuration(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return Duration.ofMinutes(1);
        }
        String tf = timeframe.trim().toLowerCase();
        try {
            if (tf.endsWith("m")) {
                int minutes = Integer.parseInt(tf.substring(0, tf.length() - 1));
                return Duration.ofMinutes(minutes);
            } else if (tf.endsWith("h")) {
                int hours = Integer.parseInt(tf.substring(0, tf.length() - 1));
                return Duration.ofHours(hours);
            } else if (tf.endsWith("d")) {
                int days = Integer.parseInt(tf.substring(0, tf.length() - 1));
                return Duration.ofDays(days);
            }
        } catch (NumberFormatException e) {
            log.warn("parseDuration: не удалось распарсить timeframe='{}', используем 1m", timeframe);
        }
        return Duration.ofMinutes(1);
    }
}
