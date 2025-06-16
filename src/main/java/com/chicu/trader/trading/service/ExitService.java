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

    /**
     * Закрыть все открытые сделки по символу: продать маркет-селлом и записать exit-поля.
     *
     * @param chatId      ID пользователя
     * @param symbol      торговая пара
     * @param exitPriceD  цена выхода (double), обернём в BigDecimal
     */
    public void exitTrade(Long chatId, String symbol, double exitPriceD) {
        // 1) находим все незакрытые логи
        List<TradeLog> openTrades = tradeLogRepository
                .findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);

        // 2) обрабатываем каждую
        for (TradeLog trade : openTrades) {
            try {
                // продать количество, уже хранящееся как BigDecimal
                orderService.placeMarketSell(chatId, symbol, trade.getQuantity());
            } catch (Exception e) {
                log.error("Exit ▶ Failed to sell: {}", e.getMessage(), e);
            }

            // 3) обновляем поля в сущности
            Instant now = Instant.now();
            BigDecimal exitPrice = BigDecimal.valueOf(exitPriceD);
            BigDecimal pnl = exitPrice
                    .subtract(trade.getEntryPrice())
                    .multiply(trade.getQuantity());

            trade.setExitTime(now);
            trade.setExitPrice(exitPrice);
            trade.setPnl(pnl);
            trade.setIsClosed(true);

            // 4) сохраняем обновлённый лог
            tradeLogRepository.save(trade);
        }
    }
}
