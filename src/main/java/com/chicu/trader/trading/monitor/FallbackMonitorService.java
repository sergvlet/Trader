package com.chicu.trader.trading.monitor;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.exit.TradingExitManager;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackMonitorService {

    private final TradeLogRepository tradeLogRepository;
    private final PriceService priceService;
    private final TradingExitManager tradingExitManager;

    /**
     * Мониторинг на случай пропуска обычного выхода.
     */
    @Scheduled(fixedRate = 15_000)
    public void monitorOpenPositions() {
        List<TradeLog> openTrades = tradeLogRepository.findAllByClosedFalse();

        for (TradeLog trade : openTrades) {
            try {
                BigDecimal currentPrice = priceService.getPrice(trade.getUserChatId(), trade.getSymbol());
                BigDecimal tp = trade.getTakeProfitPrice();
                BigDecimal sl = trade.getStopLossPrice();

                if (currentPrice == null || tp == null || sl == null) {
                    log.warn("Не удалось получить цены по сделке {} - пропуск", trade.getSymbol());
                    continue;
                }

                boolean shouldExit = currentPrice.compareTo(tp) >= 0 || currentPrice.compareTo(sl) <= 0;

                if (shouldExit) {
                    log.warn("⚠️ FallbackExit: символ={}, current={}, TP={}, SL={}",
                            trade.getSymbol(), currentPrice, tp, sl);
                    // Теперь вызываем без передачи currentPrice — метод сам его возьмет
                    tradingExitManager.forceExit(trade);
                }
            } catch (Exception e) {
                log.error("❗ Ошибка в FallbackMonitorService: {}", e.getMessage(), e);
            }
        }
    }
}
