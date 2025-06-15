package com.chicu.trader.trading.service;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExitService {

    private final TradeLogRepository tradeLogRepository;
    private final OrderService orderService;

    public void exitTrade(Long chatId, String symbol, double exitPrice) {
        List<TradeLog> openTrades = tradeLogRepository
                .findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);

        for (TradeLog trade : openTrades) {
            try {
                // Используем placeMarketSell и BigDecimal для количества
                orderService.placeMarketSell(
                        chatId,
                        symbol,
                        BigDecimal.valueOf(trade.getQuantity())
                );
            } catch (Exception e) {
                log.error("Exit ▶ Failed to sell: {}", e.getMessage(), e);
            }

            trade.setExitTime(Instant.now());
            trade.setExitPrice(exitPrice);
            trade.setPnl((exitPrice - trade.getEntryPrice()) * trade.getQuantity());
            trade.setIsClosed(true);
            tradeLogRepository.save(trade);
        }
    }
}
