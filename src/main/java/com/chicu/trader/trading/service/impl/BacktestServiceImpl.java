package com.chicu.trader.trading.service.impl;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.BacktestResult;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.BacktestService;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

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
        StrategySettings strategySettings = strategyRegistry.getSettings(settings.getStrategy(), chatId);
        TradeStrategy strategy = strategyRegistry.getStrategy(settings.getStrategy());

        Duration interval = parseTimeframe(strategySettings.getTimeframe());
        int limit = strategySettings.getCachedCandlesLimit();
        double commissionPct = settings.getCommission() != null ? settings.getCommission() : 0.1;

        List<ProfitablePair> activePairs = pairService.getActivePairs(chatId);
        BacktestResult result = new BacktestResult();

        for (ProfitablePair pair : activePairs) {
            String symbol = pair.getSymbol();
            double tpPct = pair.getTakeProfitPct() != null ? pair.getTakeProfitPct() : 2.0;
            double slPct = pair.getStopLossPct() != null ? pair.getStopLossPct() : 1.0;

            List<Candle> candles = candleService.loadHistory(symbol, interval, limit);
            if (candles.size() < 20) continue;

            boolean open = false;
            Candle entry = null;

            for (int i = 20; i < candles.size(); i++) {
                List<Candle> history = candles.subList(0, i + 1);
                SignalType signal = strategy.evaluate(history, strategySettings);
                Candle current = candles.get(i);

                if (signal == SignalType.BUY && !open) {
                    open = true;
                    entry = current;
                } else if (open && entry != null) {
                    double entryPrice = entry.getClose();
                    double high = current.getHigh();
                    double low = current.getLow();
                    double tpPrice = entryPrice * (1 + tpPct / 100.0);
                    double slPrice = entryPrice * (1 - slPct / 100.0);

                    boolean tpHit = high >= tpPrice;
                    boolean slHit = low <= slPrice;

                    if (tpHit || slHit) {
                        double exitPrice = tpHit ? tpPrice : slPrice;
                        result.addTrade(new BacktestResult.Trade(
                                symbol,
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
        if (tf == null || tf.isBlank()) return Duration.ofMinutes(1);
        tf = tf.trim().toLowerCase();
        long value = Long.parseLong(tf.substring(0, tf.length() - 1));
        char unit = tf.charAt(tf.length() - 1);

        return switch (unit) {
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + tf);
        };
    }
}
