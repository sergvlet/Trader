// src/main/java/com/chicu/trader/trading/backtest/impl/BacktestServiceImpl.java
package com.chicu.trader.trading.backtest.impl;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.backtest.BacktestResult;
import com.chicu.trader.trading.backtest.BacktestService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BacktestServiceImpl implements BacktestService {

    private final AiTradingSettingsService settingsService;
    private final ProfitablePairService pairService;
    private final CandleService candleService;
    private final StrategyRegistry strategyRegistry;

    @Override
    public BacktestResult runBacktest(Long chatId) {
        AiTradingSettings settings = settingsService.getSettingsOrThrow(chatId);
        // Настройки стратегии (тип-специфичные параметры)
        StrategySettings strategySettings =
                strategyRegistry.getSettings(settings.getStrategy(), chatId);
        TradeStrategy strategy =
                strategyRegistry.getStrategy(settings.getStrategy());

        // Timeframe и лимит свечей берём из общих настроек
        Duration interval = parseTimeframe(settings.getTimeframe());
        int limit = settings.getCachedCandlesLimit() != null
                ? settings.getCachedCandlesLimit() : 0;
        double commissionPct = settings.getCommission() != null
                ? settings.getCommission() : 0.0;

        BacktestResult result = new BacktestResult();
        List<ProfitablePair> activePairs = pairService.getActivePairs(chatId);

        for (ProfitablePair pair : activePairs) {
            List<Candle> candles = candleService.loadHistory(
                    pair.getSymbol(), interval, limit);
            if (candles == null || candles.size() < 2) continue;

            boolean open = false;
            Candle entry = null;
            double tpPct = Optional.ofNullable(pair.getTakeProfitPct()).orElse(0.0);
            double slPct = Optional.ofNullable(pair.getStopLossPct()).orElse(0.0);

            // простейший backtest с одним открытым лотом
            for (int i = 1; i < candles.size(); i++) {
                List<Candle> history = candles.subList(0, i + 1);
                SignalType signal = strategy.evaluate(history, strategySettings);
                Candle current = candles.get(i);

                if (signal == SignalType.BUY && !open) {
                    open = true;
                    entry = current;
                } else if (open && entry != null) {
                    double entryPrice = entry.getClose();
                    double tpPrice = entryPrice * (1 + tpPct / 100.0);
                    double slPrice = entryPrice * (1 - slPct / 100.0);

                    boolean tpHit = current.getHigh() >= tpPrice;
                    boolean slHit = current.getLow() <= slPrice;
                    if (tpHit || slHit) {
                        double exitPrice = tpHit ? tpPrice : slPrice;
                        result.addTrade(new BacktestResult.Trade(
                                pair.getSymbol(),
                                entry.getCloseTime(),
                                entryPrice,
                                current.getCloseTime(),
                                exitPrice,
                                commissionPct
                        ));
                        open = false;
                        entry = null;
                    }
                }
            }
        }
        return result;
    }

    private Duration parseTimeframe(String tf) {
        if (tf == null || tf.isBlank()) {
            return Duration.ofMinutes(1);
        }
        String t = tf.trim().toLowerCase();
        long val = Long.parseLong(t.substring(0, t.length() - 1));
        return switch (t.charAt(t.length() - 1)) {
            case 'm' -> Duration.ofMinutes(val);
            case 'h' -> Duration.ofHours(val);
            case 'd' -> Duration.ofDays(val);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + tf);
        };
    }
}
