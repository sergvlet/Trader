package com.chicu.trader.trading.executor;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.risk.RiskManager;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.PriceService;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
    private final PriceService priceService;
    private final TradeLogRepository tradeLogRepository;

    public void processSymbol(Long chatId, ProfitablePair pair) {
        AiTradingSettings settings = settingsService.getSettingsOrThrow(chatId);
        var strategy = strategyRegistry.getStrategy(settings.getStrategy());
        Duration interval = parseDuration(settings.getTimeframe());

        List<Candle> candles = candleService.loadHistory(
                pair.getSymbol(), interval, settings.getCachedCandlesLimit()
        );
        if (candles == null || candles.isEmpty()) {
            log.warn("❌ Нет свечей для symbol={}", pair.getSymbol());
            return;
        }

        double lastPrice = candles.get(candles.size() - 1).getClose();
        SignalType signal = strategy.evaluate(candles, settings);
        log.info("📊 Сигнал {} для symbol={} → {}", chatId, pair.getSymbol(), signal);

        if (signal == SignalType.BUY) {
            double qty = riskManager.calculatePositionSize(
                    chatId, pair.getSymbol(), lastPrice, settings
            );

            if (qty <= 0) {
                log.warn("❌ qty=0 для symbol={} — пропускаем", pair.getSymbol());
                return;
            }

            try {
                BigDecimal entryPrice = priceService.getPrice(chatId, pair.getSymbol());
                BigDecimal quantity = BigDecimal.valueOf(qty);

                String orderId = orderService.placeMarketBuy(chatId, pair.getSymbol(), quantity);

                // === Расчёт TP/SL ===
                BigDecimal tp = entryPrice.multiply(BigDecimal.valueOf(1 + pair.getTakeProfitPct() / 100.0));
                BigDecimal sl = entryPrice.multiply(BigDecimal.valueOf(1 - pair.getStopLossPct() / 100.0));

                // === OCO ===
                try {
                    orderService.placeOcoSell(chatId, pair.getSymbol(), quantity, sl, tp);
                } catch (Exception ex) {
                    log.warn("⚠️ Ошибка установки OCO: {}", ex.getMessage());
                }

                // === Сохраняем в TradeLog ===
                TradeLog logEntry = TradeLog.builder()
                        .userChatId(chatId)
                        .symbol(pair.getSymbol())
                        .entryTime(Instant.now())
                        .entryPrice(entryPrice)
                        .quantity(quantity)
                        .entryClientOrderId(orderId)
                        .takeProfitPrice(tp)
                        .stopLossPrice(sl)
                        .closed(false)
                        .build();
                tradeLogRepository.save(logEntry);

                log.info("🟢 Открыта сделка {} qty={} TP={} SL={}",
                        pair.getSymbol(), quantity, tp, sl);

            } catch (Exception e) {
                log.error("❌ Ошибка покупки: {}", e.getMessage(), e);
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
            log.warn("⚠️ Неверный timeframe='{}', используем 1m", timeframe);
        }
        return Duration.ofMinutes(1);
    }
}
