// src/main/java/com/chicu/trader/trading/StrategyFacade.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.model.TradeLog;
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

/**
 * Умный фасад для работы с любыми стратегиями.
 * Извлекает из настроек пользователя выбранный StrategyType,
 * берёт соответствующий TradeStrategy у StrategyRegistry,
 * затем для каждой «профитной» пары запрашивает исторические свечи,
 * вызывает strat.evaluate(...) и, в зависимости от сигнала, открывает/закрывает позиции.
 */
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

    /**
     * Основной метод: перебирает все переданные ProfitablePair,
     * для каждой пары вычисляет сигнал по выбранной стратегии.
     *
     * @param chatId идентификатор пользователя
     * @param pairs  список ProfitablePair (символы, TP/SL и т.п.)
     */
    public void applyStrategies(Long chatId, List<ProfitablePair> pairs) {
        // 1) Загружаем настройки AI-торговли (включая выбранный StrategyType)
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        TradeStrategy strat = registry.getByType(settings.getStrategy());
        log.info("StrategyFacade: для chatId={} выбрана стратегия {}", chatId, settings.getStrategy());

        // 2) Для каждой «профитной» пары
        for (ProfitablePair pair : pairs) {
            String symbol = pair.getSymbol();
            log.info("StrategyFacade: начинаем обработку пары symbol={} для chatId={}", symbol, chatId);

            // 2.1) Запрашиваем исторические свечи через CandleService.history(...)
            Duration interval = parseDuration(settings.getTimeframe());
            List<Candle> candles = candleService.history(
                    symbol,
                    interval,
                    settings.getCachedCandlesLimit()
            );
            if (candles == null || candles.isEmpty()) {
                log.warn("StrategyFacade: нет свечей для symbol={} chatId={}", symbol, chatId);
                continue;
            }
            log.debug("StrategyFacade: получено {} свечей для symbol={} chatId={}", candles.size(), symbol, chatId);

            // 2.2) Вычисляем сигнал у выбранной стратегии
            double lastClose = candles.get(candles.size() - 1).getClose();
            log.debug("StrategyFacade: передаем последние данные стратегии (symbol={}, lastClose={}) для chatId={}",
                    symbol, lastClose, chatId);
            TradeStrategy.SignalType signal = strat.evaluate(candles, settings);
            log.info("StrategyFacade: сигнал стратегии для symbol={} chatId={} -> {}", symbol, chatId, signal);

            // 2.3) Берём текущую цену как цену закрытия последней свечи
            double currentPrice = lastClose;

            switch (signal) {
                case BUY:
                    log.info("StrategyFacade: найден сигнал BUY для symbol={} chatId={} (price={})", symbol, chatId, currentPrice);
                    enterTrade(chatId, symbol, currentPrice, settings, pair);
                    break;
                case SELL:
                    log.info("StrategyFacade: найден сигнал SELL для symbol={} chatId={} (price={})", symbol, chatId, currentPrice);
                    exitAllTrades(chatId, symbol, currentPrice);
                    break;
                case HOLD:
                default:
                    log.debug("StrategyFacade: сигнал HOLD для symbol={} chatId={} (price={})", symbol, chatId, currentPrice);
                    // ничего не делаем
                    break;
            }
        }
    }

    /**
     * Открытие позиции: рассчитывает объём на основе riskThreshold% от свободного баланса,
     * создаёт OCO-ордер (TP + SL), сохраняет вход в TradeLog.
     *
     * @param chatId       идентификатор пользователя
     * @param symbol       торгуемый символ
     * @param price        текущая цена
     * @param settings     настройки AI-торговли пользователя
     * @param pair         объект ProfitablePair с TP/SL процентами
     */
    private void enterTrade(Long chatId,
                            String symbol,
                            double price,
                            AiTradingSettings settings,
                            ProfitablePair pair) {

        double riskPct = settings.getRiskThreshold() != null
                ? settings.getRiskThreshold()
                : 0.0;
        log.debug("enterTrade: chatId={}, symbol={}, riskPct={}", chatId, symbol, riskPct);

        // Базовый актив (например, если symbol="BTCUSDT", baseAsset="BTC")
        String baseAsset = symbol.replaceAll("[A-Z]+$", "");
        double balance = accountService.getFreeBalance(chatId, baseAsset);
        log.debug("enterTrade: свободный баланс для {} = {} {}", chatId, balance, baseAsset);

        double usdValue = balance * riskPct / 100.0;
        double qty = usdValue > 0 ? usdValue / price : 0.0;
        if (qty <= 0) {
            log.warn("enterTrade: qty=0 для chatId={}, symbol={}, balance={}, riskPct={}",
                    chatId, symbol, balance, riskPct);
            return;
        }

        // ProfitablePair.getTakeProfitPct() и getStopLossPct() возвращают примитив double:
        double tpPct = pair.getTakeProfitPct();
        double slPct = pair.getStopLossPct();
        log.debug("enterTrade: tpPct={}, slPct={} для symbol={} chatId={}", tpPct, slPct, symbol, chatId);

        // Если в ProfitablePair настроек нет (pct == 0), используем риск в качестве TP/SL
        double tpPrice = (tpPct > 0)
                ? price * (1 + tpPct / 100.0)
                : price * (1 + riskPct / 100.0);
        double slPrice = (slPct > 0)
                ? price * (1 - slPct / 100.0)
                : price * (1 - riskPct / 100.0);

        log.info("enterTrade: рассчитываем TP={} SL={} для symbol={} chatId={}", tpPrice, slPrice, symbol, chatId);

        // Размещаем OCO-ордер: SL + TP
        orderService.placeOcoOrder(chatId, symbol, qty, slPrice, tpPrice);
        log.info("enterTrade: отправлен OCO-ордер chatId={}, symbol={}, qty={}, SL={}, TP={}",
                chatId, symbol, qty, slPrice, tpPrice);

        // Сохраняем запись в TradeLog
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

    /**
     * Закрытие всех незакрытых сделок по символу:
     * если сработал TP/SL или пришёл SELL-сигнал, принудительно закрываем.
     *
     * @param chatId    идентификатор пользователя
     * @param symbol    торгуемый символ
     * @param exitPrice цена закрытия
     */
    private void exitAllTrades(Long chatId, String symbol, double exitPrice) {
        log.debug("exitAllTrades: chatId={}, symbol={}, exitPrice={}", chatId, symbol, exitPrice);

        List<TradeLog> openTrades = tradeLogRepository
                .findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        log.debug("exitAllTrades: найдено {} открытых сделок для symbol={} chatId={}", openTrades.size(), symbol, chatId);

        for (TradeLog open : openTrades) {
            double entryPrice = open.getEntryPrice();
            double qty = open.getQuantity();
            double tp = open.getTakeProfitPrice();   // возвращает примитив double
            double sl = open.getStopLossPrice();     // возвращает примитив double

            boolean hitTp = (tp > 0) && (exitPrice >= tp);
            boolean hitSl = (sl > 0) && (exitPrice <= sl);
            // При SELL-сигнале – force-close даже без TP/SL
            if (!hitTp && !hitSl) {
                log.info("exitAllTrades: force-close (SELL-сигнал) для chatId={}, symbol={}", chatId, symbol);
            }

            // Размещаем рыночный ордер на закрытие
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

    /**
     * Преобразует строковый таймфрейм ("1m", "5m", "1h", "4h", "1d" и т. д.)
     * в объект Duration. Если строка нераспознана, по умолчанию возвращает Duration.ofMinutes(1).
     */
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
