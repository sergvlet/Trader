package com.chicu.trader.trading.exit;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.PriceService;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingExitManager {

    private final TradeLogRepository tradeLogRepository;
    private final OrderService       orderService;
    private final PriceService       priceService;

    // Проценты TP/SL из лога (брать не из here) игнорируем, т.к. ордеры OCO уже выставлены

    /**
     * Проверяем открытые сделки каждые 30 секунд и закрываем MARKET SELL’ом,
     * если OCO не сработал.
     */
    @Scheduled(fixedDelay = 30_000)
    public void monitorOpenTrades() {
        List<TradeLog> openTrades = tradeLogRepository.findAllByClosedFalse();

        for (TradeLog trade : openTrades) {
            try {
                String symbol = trade.getSymbol();
                Long   chatId = trade.getUserChatId();

                BigDecimal entryPrice = trade.getEntryPrice();
                BigDecimal quantity   = trade.getQuantity();

                // Текущая рыночная цена
                BigDecimal currentPrice = priceService.getPrice(chatId, symbol);
                if (currentPrice == null) {
                    log.warn("Не удалось получить текущую цену для {}", symbol);
                    continue;
                }

                // Проверяем, не прошла ли цена TP/SL
                BigDecimal tp = trade.getTakeProfitPrice().setScale(8, RoundingMode.HALF_UP);
                BigDecimal sl = trade.getStopLossPrice().setScale(8, RoundingMode.HALF_UP);

                boolean hitTp = currentPrice.compareTo(tp) >= 0;
                boolean hitSl = currentPrice.compareTo(sl) <= 0;

                if (hitTp || hitSl) {
                    log.info("Закрываем {}: current={} TP={} SL={}", symbol, currentPrice, tp, sl);

                    // MARKET SELL
                    String exitClientOrderId = orderService.placeMarketSell(chatId, symbol, quantity);

                    // Рассчитываем PnL
                    BigDecimal pnl = currentPrice
                            .subtract(entryPrice)
                            .multiply(quantity)
                            .setScale(8, RoundingMode.HALF_UP);

                    // Обновляем лог
                    trade.setClosed(true);
                    trade.setExitTime(Instant.now());
                    trade.setExitPrice(currentPrice);
                    trade.setExitClientOrderId(exitClientOrderId);
                    trade.setPnl(pnl);

                    tradeLogRepository.save(trade);

                    log.info("🔴 CLOSED {} exitId={} price={} pnl={}", symbol, exitClientOrderId, currentPrice, pnl);
                }
            } catch (Exception e) {
                log.error("Ошибка при мониторинге сделки {}: {}", trade.getSymbol(), e.getMessage(), e);
            }
        }
    }

    /**
     * Принудительное закрытие сделки (можно вызвать вручную).
     */
    public void forceExit(TradeLog trade) {
        try {
            Long chatId = trade.getUserChatId();
            String symbol = trade.getSymbol();
            BigDecimal currentPrice = priceService.getPrice(chatId, symbol);
            if (currentPrice == null) {
                log.warn("Не удалось получить цену для forceExit {}", symbol);
                return;
            }

            BigDecimal quantity = trade.getQuantity();
            String exitClientOrderId = orderService.placeMarketSell(chatId, symbol, quantity);

            BigDecimal pnl = currentPrice
                    .subtract(trade.getEntryPrice())
                    .multiply(quantity)
                    .setScale(8, RoundingMode.HALF_UP);

            trade.setClosed(true);
            trade.setExitTime(Instant.now());
            trade.setExitPrice(currentPrice);
            trade.setExitClientOrderId(exitClientOrderId);
            trade.setPnl(pnl);

            tradeLogRepository.save(trade);

            log.info("✅ Force exit {} exitId={} price={} pnl={}",
                    symbol, exitClientOrderId, currentPrice, pnl);
        } catch (Exception e) {
            log.error("Ошибка forceExit для {}: {}", trade.getSymbol(), e.getMessage(), e);
        }
    }
}
