package com.chicu.trader.trading.reentry;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.risk.RiskManager;
import com.chicu.trader.trading.service.AccountService;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReEntryService {

    private final AiTradingSettingsService settingsService;
    private final TradeLogRepository tradeLogRepository;
    private final CandleService candleService;
    private final StrategyRegistry strategyRegistry;
    private final RiskManager riskManager;
    private final OrderService orderService;
    private final AccountService accountService;

    @Scheduled(fixedRate = 300_000)  // каждые 5 минут
    public void checkAndReenter() {
        List<Long> allUsers = settingsService.findAllChatIds();

        for (Long chatId : allUsers) {
            try {
                processUser(chatId);
            } catch (Exception e) {
                log.error("❌ AutoReEntry ▶ ошибка для chatId={}: {}", chatId, e.getMessage(), e);
            }
        }
    }

    private void processUser(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        if (!Boolean.TRUE.equals(settings.getIsRunning())) {
            log.info("⏸ AutoReEntry ▶ торговля отключена для chatId={}", chatId);
            return;
        }

        var strategyType = settings.getStrategy();
        TradeStrategy strategy = strategyRegistry.getStrategy(strategyType);
        StrategySettings strategySettings = strategyRegistry.getSettings(strategyType, chatId);

        List<TradeLog> closedProfitable = tradeLogRepository.findRecentClosedProfitableTrades(chatId);
        for (TradeLog trade : closedProfitable) {
            String symbol = trade.getSymbol();

            Duration timeframe = parseDuration(settings.getTimeframe());
            List<Candle> candles = candleService.loadHistory(symbol, timeframe, settings.getCachedCandlesLimit());
            if (candles == null || candles.isEmpty()) {
                log.warn("⚠️ AutoReEntry ▶ нет свечей для {}", symbol);
                continue;
            }

            SignalType signal = strategy.evaluate(candles, strategySettings);
            log.info("📈 AutoReEntry ▶ сигнал {} для {} = {}", strategyType, symbol, signal);

            if (signal == SignalType.BUY) {
                double lastPrice = candles.get(candles.size() - 1).getClose();
                double qty = riskManager.calculatePositionSize(chatId, symbol, lastPrice, settings);
                if (qty > 0) {
                    try {
                        orderService.placeMarketBuy(chatId, symbol, BigDecimal.valueOf(qty));
                        log.info("🟢 AutoReEntry ▶ Перезаход BUY по {} qty={}", symbol, qty);
                    } catch (Exception e) {
                        log.error("❌ AutoReEntry ▶ ошибка покупки: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }

    private Duration parseDuration(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return Duration.ofMinutes(1);
        }
        try {
            timeframe = timeframe.trim().toLowerCase();
            if (timeframe.endsWith("m")) return Duration.ofMinutes(Integer.parseInt(timeframe.replace("m", "")));
            if (timeframe.endsWith("h")) return Duration.ofHours(Integer.parseInt(timeframe.replace("h", "")));
            if (timeframe.endsWith("d")) return Duration.ofDays(Integer.parseInt(timeframe.replace("d", "")));
        } catch (Exception e) {
            log.warn("⚠️ AutoReEntry ▶ неверный timeframe='{}', используем 1m", timeframe);
        }
        return Duration.ofMinutes(1);
    }
}
