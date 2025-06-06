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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StrategyFacade {

    private final StrategyRegistry         registry;
    private final AiTradingSettingsService settingsService;
    private final CandleService            candleService;
    private final AccountService           accountService;
    private final OrderService             orderService;
    private final TradeLogRepository       tradeLogRepository;

    public void applyStrategies(Long chatId, List<ProfitablePair> pairs) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        TradeStrategy strat = registry.getByType(settings.getStrategy());
        log.info("StrategyFacade ▶ для chatId={} выбрана стратегия={}", chatId, settings.getStrategy());

        for (ProfitablePair pair : pairs) {
            String symbol = pair.getSymbol();
            log.info("StrategyFacade ▶ начинаем обработку пары symbol={} для chatId={}", symbol, chatId);

            Duration interval = parseDuration(settings.getTimeframe());
            List<Candle> candles = candleService.history(symbol, interval, settings.getCachedCandlesLimit());
            if (candles == null || candles.isEmpty()) {
                log.warn("StrategyFacade ▶ нет свечей для symbol={} chatId={}", symbol, chatId);
                continue;
            }

            log.debug("StrategyFacade ▶ получено {} свечей для symbol={} chatId={}", candles.size(), symbol, chatId);

            double lastClose = candles.get(candles.size() - 1).getClose();
            SignalType signal = strat.evaluate(candles, settings);
            log.info("StrategyFacade ▶ сигнал стратегии для symbol={} chatId={} → {}", symbol, chatId, signal);

            double currentPrice = lastClose;
            switch (signal) {
                case BUY -> {
                    log.info("StrategyFacade ▶ найден сигнал BUY для symbol={} chatId={} (price={})",
                            symbol, chatId, currentPrice);
                    enterTrade(chatId, symbol, currentPrice, settings, pair);
                }
                case SELL -> {
                    log.info("StrategyFacade ▶ найден сигнал SELL для symbol={} chatId={} (price={})",
                            symbol, chatId, currentPrice);
                    exitAllTrades(chatId, symbol, currentPrice);
                }
                case HOLD -> {
                    log.debug("StrategyFacade ▶ сигнал HOLD для symbol={} chatId={} (price={})",
                            symbol, chatId, currentPrice);
                }
            }
        }
    }

    private void enterTrade(Long chatId,
                            String symbol,
                            double price,
                            AiTradingSettings settings,
                            ProfitablePair pair) {

        double riskPct = settings.getRiskThreshold() != null
                ? settings.getRiskThreshold()
                : 0.0;
        log.debug("enterTrade ▶ chatId={}, symbol={}, riskPct={}", chatId, symbol, riskPct);

        // 1) Определяем quoteAsset
        String quoteAsset;
        if (symbol.endsWith("USDT")) {
            quoteAsset = "USDT";
        } else if (symbol.endsWith("BUSD")) {
            quoteAsset = "BUSD";
        } else if (symbol.endsWith("BTC")) {
            quoteAsset = "BTC";
        } else if (symbol.endsWith("ETH")) {
            quoteAsset = "ETH";
        } else {
            quoteAsset = symbol.replaceFirst("^[A-Z]+", "");
        }

        // 2) Узнаём свободный баланс quoteAsset
        double quoteBalance = accountService.getFreeBalance(chatId, quoteAsset);
        log.debug("enterTrade ▶ свободный баланс для chatId={} asset={} = {}", chatId, quoteAsset, quoteBalance);

        // 3) Сколько тратим из quoteBalance
        double amountToSpend = quoteBalance * (riskPct / 100.0);
        if (amountToSpend <= 0) {
            log.warn("enterTrade ▶ amountToSpend=0 (quoteBalance={} riskPct={}) для chatId={}, symbol={}",
                    quoteBalance, riskPct, chatId, symbol);
            return;
        }

        // 4) rawQty = amountToSpend / price
        double rawQty = amountToSpend / price;
        log.debug("enterTrade ▶ рассчитано rawQty = {}", rawQty);

        // 5) Обрезаем qty до 3 знаков (DOWN)
        double qty = roundQuantity(rawQty);
        if (qty <= 0) {
            log.warn("enterTrade ▶ qty после округления = 0 (rawQty={}) для chatId={}, symbol={}",
                    rawQty, chatId, symbol);
            return;
        }
        log.debug("enterTrade ▶ qty после truncate (3 знака) = {}", qty);

        // 6) Рассчитываем “сырые” TP/SL на основе процентов
        double tpPct = pair.getTakeProfitPct();
        double slPct = pair.getStopLossPct();
        log.debug("enterTrade ▶ tpPct={}, slPct={} для symbol={} chatId={}",
                tpPct, slPct, symbol, chatId);

        double rawTpPrice = (tpPct > 0)
                ? price * (1 + tpPct / 100.0)
                : price * (1 + riskPct / 100.0);
        double rawSlPrice = (slPct > 0)
                ? price * (1 - slPct / 100.0)
                : price * (1 - riskPct / 100.0);

        // 7) Обрезаем TP/SL до 2 знаков (DOWN)
        double tpPrice = roundPrice(rawTpPrice);
        double slPrice = roundPrice(rawSlPrice);
        log.info("enterTrade ▶ рассчитываем TP={} SL={} для symbol={} chatId={}",
                tpPrice, slPrice, symbol, chatId);

        // --- Проверяем корректность отношений для OCO:
        //    нужно, чтобы tpPrice > price > slPrice
        if (!(tpPrice > price && price > slPrice)) {
            log.error("enterTrade ▶ неверное соотношение цен для OCO — пропускаем OCO. price={}, slPrice={}, tpPrice={}",
                    price, slPrice, tpPrice);
            // Всё равно продолжаем вход (MARKET), но без OCO‐защиты
        }

        // === ШАГ 1: MARKET BUY ===
        log.info("enterTrade ▶ пытаемся отправить рыночный ордер BUY chatId={} symbol={} qty={}",
                chatId, symbol, qty);
        try {
            orderService.placeMarketOrder(chatId, symbol, qty);
            log.info("enterTrade ▶ MarketOrder (BUY) отправлен успешно chatId={} symbol={} qty={}",
                    chatId, symbol, qty);
        } catch (Exception e) {
            log.error("enterTrade ▶ Binance вернул ошибку при MarketOrder (BUY) chatId={} symbol={} qty={}: {}",
                    chatId, symbol, qty, e.getMessage(), e);
            return;
        }

        // === ШАГ 2: OCO SELL (защита TP/SL) ===
        log.info("enterTrade ▶ пытаемся отправить OCO-ордер SELL chatId={} symbol={} qty={} SL={} TP={}",
                chatId, symbol, qty, slPrice, tpPrice);
        try {
            orderService.placeOcoOrder(chatId, symbol, qty, slPrice, tpPrice);
            log.info("enterTrade ▶ OCO-ордер (SELL) отправлен успешно chatId={} symbol={} qty={} SL={} TP={}",
                    chatId, symbol, qty, slPrice, tpPrice);
        } catch (Exception e) {
            log.error("enterTrade ▶ Binance вернул ошибку при OCO-ордере SELL chatId={} symbol={} qty={} SL={} TP={}: {}",
                    chatId, symbol, qty, slPrice, tpPrice, e.getMessage(), e);
            // Сделка уже открыта, но без OCO‐защиты
        }

        // === ШАГ 3: сохраняем запись в TradeLog ===
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

        log.info("enterTrade ▶ Entered trade chatId={} symbol={} qty={} entryPrice={} TP={} SL={}",
                chatId, symbol, qty, price, tpPrice, slPrice);
    }

    private void exitAllTrades(Long chatId, String symbol, double exitPrice) {
        log.debug("exitAllTrades ▶ chatId={}, symbol={}, exitPrice={}", chatId, symbol, exitPrice);

        List<TradeLog> openTrades = tradeLogRepository
                .findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        log.debug("exitAllTrades ▶ найдено {} открытых сделок для symbol={} chatId={}",
                openTrades.size(), symbol, chatId);

        for (TradeLog open : openTrades) {
            double entryPrice = open.getEntryPrice();
            double qty = open.getQuantity();
            double tp = open.getTakeProfitPrice();
            double sl = open.getStopLossPrice();

            boolean hitTp = (tp > 0) && (exitPrice >= tp);
            boolean hitSl = (sl > 0) && (exitPrice <= sl);
            if (!hitTp && !hitSl) {
                log.info("exitAllTrades ▶ force-close (SELL-сигнал) для chatId={} symbol={}", chatId, symbol);
            }

            log.info("exitAllTrades ▶ пытаемся отправить рыночный ордер SELL chatId={} symbol={} qty={}",
                    chatId, symbol, qty);
            try {
                orderService.placeMarketOrder(chatId, symbol, qty);
                log.info("exitAllTrades ▶ MarketOrder (SELL) отправлен успешно chatId={} symbol={} qty={}",
                        chatId, symbol, qty);
            } catch (Exception e) {
                log.error("exitAllTrades ▶ Binance вернул ошибку при MarketOrder (SELL) chatId={} symbol={} qty={}: {}",
                        chatId, symbol, qty, e.getMessage(), e);
                // продолжаем закрывать остальные
            }

            open.setExitTime(Instant.now());
            open.setClosed(true);
            open.setExitPrice(exitPrice);
            open.setPnl((exitPrice - entryPrice) * qty);
            tradeLogRepository.save(open);

            String reason = hitTp ? "TP" : (hitSl ? "SL" : "SELL_SIGNAL");
            log.info("exitAllTrades ▶ Exited trade chatId={} symbol={} qty={} exitPrice={} PnL={} reason={}",
                    chatId, symbol, qty, open.getExitPrice(), open.getPnl(), reason);
        }
    }

    /** Обрезает qty до 3 знаков после запятой (RoundingMode.DOWN). */
    private double roundQuantity(double qty) {
        BigDecimal bd = new BigDecimal(qty);
        return bd.setScale(3, RoundingMode.DOWN).doubleValue();
    }

    /** Обрезает price до 2 знаков после запятой (RoundingMode.DOWN). */
    private double roundPrice(double price) {
        BigDecimal bd = new BigDecimal(price);
        return bd.setScale(2, RoundingMode.DOWN).doubleValue();
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
            log.warn("parseDuration ▶ не удалось распарсить timeframe='{}', используем 1m", timeframe);
        }
        return Duration.ofMinutes(1);
    }
}
