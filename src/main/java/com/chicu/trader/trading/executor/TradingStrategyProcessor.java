package com.chicu.trader.trading.executor;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.risk.RiskManager;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingStrategyProcessor {

    private final StrategyRegistry strategyRegistry;
    private final AiTradingSettingsService settingsService;
    private final CandleService candleService;
    private final RiskManager riskManager;
    private final OrderService orderService;

    public void processSymbol(Long chatId, ProfitablePair pair) {
        AiTradingSettings settings = settingsService.getSettingsOrThrow(chatId);
        var strategy = strategyRegistry.getStrategy(settings.getStrategy());
        Duration interval = parseDuration(settings.getTimeframe());

        List<Candle> candles = candleService.loadHistory(
                pair.getSymbol(), interval, settings.getCachedCandlesLimit()
        );
        if (candles.isEmpty()) {
            log.warn("Нет свечей для symbol={}", pair.getSymbol());
            return;
        }

        double lastPrice = candles.get(candles.size() - 1).getClose();
        SignalType signal = strategy.evaluate(candles, settings);
        log.info("Сигнал {} для symbol={} → {}", chatId, pair.getSymbol(), signal);

        if (signal == SignalType.BUY) {
            double qty = riskManager.calculatePositionSize(
                    chatId, pair.getSymbol(), lastPrice, settings
            );
            if (qty > 0) {
                try {
                    // Заменили placeMarketOrder на placeMarketBuy и обёртку BigDecimal
                    orderService.placeMarketBuy(
                            chatId,
                            pair.getSymbol(),
                            BigDecimal.valueOf(qty)
                    );
                    log.info("Куплено {} qty={}", pair.getSymbol(), qty);
                } catch (Exception e) {
                    log.error("Ошибка покупки: {}", e.getMessage());
                }
            }
        }
        // при необходимости можно добавить обработку SELL:
        // else if (signal == SignalType.SELL) { … placeMarketSell … }
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
            log.warn("Неверный timeframe='{}', используем 1m", timeframe);
        }
        return Duration.ofMinutes(1);
    }
}
