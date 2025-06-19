package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.risk.RiskManager;
import com.chicu.trader.trading.service.PriceService;
import com.chicu.trader.trading.service.binance.OrderService;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.util.QuantityAdjuster;
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
    private final PriceService             priceService;
    private final RiskManager               riskManager;
    private final OrderService              orderService;
    private final BinanceRestClientFactory  clientFactory;
    private final TradeLogRepository        repo;

    public void apply(Long chatId, List<ProfitablePair> pairs) {
        // 1) Настройки пользователя
        AiTradingSettings settings = settingsService.getSettingsOrThrow(chatId);

        // 2) Один раз получаем все ограничения рынка
        ExchangeInfo exchangeInfo =
                clientFactory.getClient(chatId).getExchangeInfo();

        for (ProfitablePair p : pairs) {
            String symbol = p.getSymbol();
            try {
                // 3) Текущая цена
                BigDecimal price = priceService.getPrice(chatId, symbol);
                if (price == null) {
                    log.warn("Не удалось получить цену для {}", symbol);
                    continue;
                }

                // 4) Размер позиции и корректировка по шагу
                double rawSize = riskManager.calculatePositionSize(
                        chatId, symbol, price.doubleValue(), settings
                );
                BigDecimal qty = QuantityAdjuster.adjustQuantity(
                        symbol, BigDecimal.valueOf(rawSize), exchangeInfo
                );
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("После коррекции qty={} для {} слишком мал — пропускаем", rawSize, symbol);
                    continue;
                }

                // 5) MARKET BUY → получаем clientOrderId
                String entryId = orderService.placeMarketBuy(chatId, symbol, qty);

                // 6) Рассчитываем TP/SL
                BigDecimal tp = price.multiply(
                        BigDecimal.valueOf(1 + p.getTakeProfitPct() / 100.0)
                );
                BigDecimal sl = price.multiply(
                        BigDecimal.valueOf(1 - p.getStopLossPct() / 100.0)
                );

                // 7) Сохраняем вход в БД, включая TP/SL
                TradeLog logEntry = TradeLog.builder()
                        .userChatId(chatId)
                        .symbol(symbol)
                        .entryTime(Instant.now())
                        .entryPrice(price)
                        .quantity(qty)
                        .entryClientOrderId(entryId)
                        .takeProfitPrice(tp)
                        .stopLossPrice(sl)
                        .closed(false)
                        .build();
                repo.save(logEntry);

                log.info("🟢 BUY {} @{} qty={} entryId={}", symbol, price, qty, entryId);

                // 8) Ставим OCO-ордер и получаем exitClientOrderId
                String exitId = orderService.placeOcoSell(chatId, symbol, qty, sl, tp);

                // 9) Фиксируем данные выхода сразу же:
                BigDecimal exitPrice = priceService.getPrice(chatId, symbol);
                Instant    exitTime  = Instant.now();
                BigDecimal pnl       = exitPrice.subtract(price).multiply(qty);

                logEntry.setExitClientOrderId(exitId);
                logEntry.setExitTime(exitTime);
                logEntry.setExitPrice(exitPrice);
                logEntry.setPnl(pnl);
                logEntry.setClosed(true);
                repo.save(logEntry);

                log.info("↗ EXIT {} exitId={} price={} pnl={}",
                        symbol, exitId, exitPrice, pnl);

            } catch (Exception ex) {
                log.error("Ошибка при обработке {}: {}", symbol, ex.getMessage(), ex);
            }
        }
    }
}
