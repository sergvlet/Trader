package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.risk.RiskManager;
import com.chicu.trader.trading.service.PriceService;
import com.chicu.trader.trading.service.ProfitablePairService;
import com.chicu.trader.trading.service.binance.OrderService;
import com.chicu.trader.trading.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeOrchestrator {

    private final AiTradingSettingsService settingsService;
    private final ProfitablePairService pairService;
    private final RiskManager riskManager;
    private final PriceService priceService;
    private final OrderService orderService;
    private final TradeLogRepository tradeLogRepository;

    public void apply(Long chatId, List<ProfitablePair> pairs) {
        AiTradingSettings settings = settingsService.getSettingsOrThrow(chatId);

        for (ProfitablePair pair : pairs) {
            try {
                String symbol = pair.getSymbol();

                BigDecimal entryPrice = priceService.getPrice(chatId, symbol);
                if (entryPrice == null) {
                    log.warn("Не удалось получить цену для {}", symbol);
                    continue;
                }

                // Вычисляем размер позиции по реальной сигнатуре
                double positionSize = riskManager.calculatePositionSize(
                        chatId,
                        symbol,
                        entryPrice.doubleValue(),
                        settings
                );
                BigDecimal qty = BigDecimal.valueOf(positionSize);

                // Ставим OCO ордер
                BigDecimal tpPrice = entryPrice.multiply(
                        BigDecimal.valueOf(1.0 + pair.getTakeProfitPct() / 100.0)
                );
                BigDecimal slPrice = entryPrice.multiply(
                        BigDecimal.valueOf(1.0 - pair.getStopLossPct() / 100.0)
                );

                orderService.placeOcoSell(chatId, symbol, qty, slPrice, tpPrice);

                // Логируем сделку
                TradeLog logEntry = TradeLog.builder()
                        .userChatId(chatId)
                        .symbol(symbol)
                        .entryTime(Instant.now())
                        .entryPrice(BigDecimal.valueOf(entryPrice.doubleValue()))
                        .quantity(BigDecimal.valueOf(qty.doubleValue()))
                        .takeProfitPrice(tpPrice)
                        .stopLossPrice(slPrice)
                        .isClosed(false)
                        .build();
                tradeLogRepository.save(logEntry);

                log.info("🟢 BUY: {} qty={} entry={} TP={} SL={}",
                        symbol, qty, entryPrice, tpPrice, slPrice);

            } catch (Exception e) {
                log.error("Ошибка обработки пары {}: {}", pair.getSymbol(), e.getMessage(), e);
            }
        }
    }
}
