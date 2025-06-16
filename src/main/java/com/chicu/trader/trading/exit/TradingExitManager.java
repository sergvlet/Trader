package com.chicu.trader.trading.exit;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
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
    private final OrderService orderService;

    // Для примера — проценты TP/SL
    private final double tpPct = 0.5;  // 0.5% take-profit
    private final double slPct = 0.3;  // 0.3% stop-loss

    /**
     * Планировщик выхода по TP/SL каждые 30 сек
     */
    @Scheduled(fixedDelay = 30_000)
    public void monitorOpenTrades() {
        List<TradeLog> openTrades = tradeLogRepository.findAllByIsClosedFalse();

        for (TradeLog trade : openTrades) {
            try {
                String symbol = trade.getSymbol();
                Long chatId = trade.getUserChatId();

                // entryPrice и количество уже хранятся как BigDecimal
                BigDecimal entryPrice  = trade.getEntryPrice();
                BigDecimal quantity    = trade.getQuantity();

                // считаем уровни TP/SL
                BigDecimal tp = entryPrice
                        .multiply(BigDecimal.valueOf(1.0 + tpPct / 100.0))
                        .setScale(8, RoundingMode.HALF_UP);
                BigDecimal sl = entryPrice
                        .multiply(BigDecimal.valueOf(1.0 - slPct / 100.0))
                        .setScale(8, RoundingMode.HALF_UP);

                // получаем текущую цену
                BigDecimal currentPrice = orderService.getLastPrice(chatId, symbol);
                if (currentPrice == null) {
                    continue;
                }

                boolean hitTp = currentPrice.compareTo(tp) >= 0;
                boolean hitSl = currentPrice.compareTo(sl) <= 0;

                if (hitTp || hitSl) {
                    log.info("Закрываем сделку по {}: current={} TP={} SL={}",
                            symbol, currentPrice, tp, sl);

                    // маркет-селл нужного количества
                    orderService.placeMarketSell(chatId, symbol, quantity);

                    // считаем PnL = (exit – entry) * qty
                    BigDecimal pnl = currentPrice
                            .subtract(entryPrice)
                            .multiply(quantity);

                    // обновляем лог и сохраняем
                    trade.setIsClosed(true);
                    trade.setExitTime(Instant.now());
                    trade.setExitPrice(currentPrice);
                    trade.setPnl(pnl);

                    tradeLogRepository.save(trade);
                }
            } catch (Exception e) {
                log.error("Ошибка обработки сделки {}: {}",
                        trade.getSymbol(), e.getMessage(), e);
            }
        }
    }

    /**
     * Принудительный выход — для Fallback-сценариев
     */
    public void forceExit(TradeLog trade, BigDecimal currentPrice) {
        try {
            Long chatId = trade.getUserChatId();
            BigDecimal quantity = trade.getQuantity();

            orderService.placeMarketSell(chatId, trade.getSymbol(), quantity);

            BigDecimal pnl = currentPrice
                    .subtract(trade.getEntryPrice())
                    .multiply(quantity);

            trade.setIsClosed(true);
            trade.setExitTime(Instant.now());
            trade.setExitPrice(currentPrice);
            trade.setPnl(pnl);

            tradeLogRepository.save(trade);

            log.info("✅ Fallback exit: {} по цене {}",
                    trade.getSymbol(), currentPrice);
        } catch (Exception e) {
            log.error("❗ Ошибка при forceExit: {}", e.getMessage(), e);
        }
    }
}
