package com.chicu.trader.trading.backtest.impl;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.backtest.BacktestResult;
import com.chicu.trader.trading.backtest.service.BacktestService;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.BacktestSettings;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BacktestServiceImpl implements BacktestService {

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceImpl.class);

    private final AiTradingSettingsService settingsService;
    private final ProfitablePairService    pairService;
    private final CandleService            candleService;
    private final StrategyRegistry         strategyRegistry;
    private final BacktestSettingsService  backtestSettingsService;

    @Override
    public BacktestResult runBacktest(Long chatId) {
        log.info("=== Starting backtest for chatId={} ===", chatId);

        // 1) Пользовательские настройки
        AiTradingSettings aiSettings = settingsService.getSettingsOrThrow(chatId);
        BacktestSettings  btSettings = backtestSettingsService.getOrCreate(chatId);

        double commissionPct = btSettings.getCommissionPct();
        double slippagePct   = btSettings.getSlippagePct();       // новый параметр
        double costPct       = commissionPct + slippagePct;       // объединённые издержки

        log.info("Using strategy='{}', symbols='{}'",
                aiSettings.getStrategy(), aiSettings.getSymbols());
        log.info("Backtest settings: timeframe='{}', limit={}, commissionPct={}%, slippagePct={}%, dateRange={}→{}",
                btSettings.getTimeframe(),
                btSettings.getCachedCandlesLimit(),
                commissionPct,
                slippagePct,
                btSettings.getStartDate(),
                btSettings.getEndDate());

        // 2) Стратегия
        StrategySettings strategySettings =
                strategyRegistry.getSettings(aiSettings.getStrategy(), chatId);
        TradeStrategy strategy =
                strategyRegistry.getStrategy(aiSettings.getStrategy());

        // 3) Параметры бэктеста
        Duration  interval  = parseTimeframe(btSettings.getTimeframe());
        int       limit     = btSettings.getCachedCandlesLimit();
        LocalDate startDate = btSettings.getStartDate();
        LocalDate endDate   = btSettings.getEndDate();

        // 4) Список символов
        List<String> symbolsToTest;
        if (aiSettings.getSymbols() != null && !aiSettings.getSymbols().isBlank()) {
            symbolsToTest = Arrays.stream(aiSettings.getSymbols().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            symbolsToTest = pairService.getActivePairs(chatId).stream()
                    .map(ProfitablePair::getSymbol)
                    .collect(Collectors.toList());
        }

        if (symbolsToTest.isEmpty()) {
            log.warn("No symbols to test for chatId={}", chatId);
        } else {
            log.info("Symbols to test: {}", symbolsToTest);
        }

        BacktestResult result = new BacktestResult();

        // 5) Цикл по символам
        for (String symbol : symbolsToTest) {
            log.info(">> Testing symbol='{}'", symbol);

            List<Candle> rawCandles = candleService.loadHistory(symbol, interval, limit);
            log.info("Loaded {} raw candles for '{}'", rawCandles.size(), symbol);

            List<Candle> candles = filterByDateRange(rawCandles, startDate, endDate);
            log.info("{} candles after filter [{}→{}] for '{}'",
                    candles.size(), startDate, endDate, symbol);

            if (candles.size() < 2) {
                log.warn("Skipping '{}': only {} candles", symbol, candles.size());
                continue;
            }

            boolean open  = false;
            Candle  entry = null;

            // TP/SL из активной ProfitablePair
            Optional<ProfitablePair> pairOpt = pairService
                    .getPairsBySymbol(chatId, symbol).stream()
                    .filter(ProfitablePair::getActive)
                    .findFirst();
            double tpPct = pairOpt.map(ProfitablePair::getTakeProfitPct).orElse(2.0);
            double slPct = pairOpt.map(ProfitablePair::getStopLossPct).orElse(1.0);

            // 6) Цикл бэктеста по свечам
            for (int i = 1; i < candles.size(); i++) {
                List<Candle> history = candles.subList(0, i + 1);
                SignalType   signal  = strategy.evaluate(history, strategySettings);
                Candle       current = candles.get(i);

                log.debug("Signal for '{}' at {}: {}",
                        symbol, Instant.ofEpochMilli(current.getCloseTime()), signal);

                if (signal == SignalType.BUY && !open) {
                    open  = true;
                    entry = current;
                    log.info("Opened BUY '{}' at {} (time={})",
                            symbol, entry.getClose(), Instant.ofEpochMilli(entry.getCloseTime()));
                } else if (open && entry != null) {
                    double entryPrice = entry.getClose();
                    double high       = current.getHigh();
                    double low        = current.getLow();
                    double tpPrice    = entryPrice * (1 + tpPct / 100.0);
                    double slPrice    = entryPrice * (1 - slPct / 100.0);

                    boolean tpHit = high >= tpPrice;
                    boolean slHit = low  <= slPrice;
                    if (tpHit || slHit) {
                        double exitPrice = tpHit ? tpPrice : slPrice;
                        result.addTrade(new BacktestResult.Trade(
                                symbol,
                                entry.getCloseTime(),
                                entryPrice,
                                current.getCloseTime(),
                                exitPrice,
                                costPct                   // передаем объединенные издержки
                        ));
                        log.info("Closed '{}' at {} (time={}), TP={}, SL={}",
                                symbol,
                                exitPrice,
                                Instant.ofEpochMilli(current.getCloseTime()),
                                tpHit,
                                slHit
                        );
                        open  = false;
                        entry = null;
                    }
                }
            }
        }

        log.info("=== Backtest complete for chatId={}, total trades={} ===",
                chatId, result.getTotalTrades());
        return result;
    }

    // Вспомогательные

    private Duration parseTimeframe(String tf) {
        if (tf == null || tf.isBlank()) return Duration.ofMinutes(1);
        tf = tf.trim().toLowerCase();
        long v = Long.parseLong(tf.substring(0, tf.length() - 1));
        return switch (tf.charAt(tf.length() - 1)) {
            case 'm' -> Duration.ofMinutes(v);
            case 'h' -> Duration.ofHours(v);
            case 'd' -> Duration.ofDays(v);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + tf);
        };
    }

    private List<Candle> filterByDateRange(
            List<Candle> rawCandles,
            LocalDate    startDate,
            LocalDate    endDate
    ) {
        ZoneId zone          = ZoneId.systemDefault();
        Instant startInstant = startDate.atStartOfDay(zone).toInstant();
        Instant endInstant   = endDate.plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .minusNanos(1);

        return rawCandles.stream()
                .filter(c -> {
                    Instant ts = Instant.ofEpochMilli(c.getCloseTime());
                    return !ts.isBefore(startInstant) && !ts.isAfter(endInstant);
                })
                .collect(Collectors.toList());
    }
}
