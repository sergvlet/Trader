package com.chicu.trader.trading.service;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeLoggerService {

    private final TradeLogRepository tradeLogRepository;

    /**
     * Проверяем — есть ли открытая позиция по символу
     */
    public boolean hasOpenPosition(Long chatId, String symbol) {
        return tradeLogRepository.existsByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
    }

    /**
     * Сохраняем новую открытую сделку
     */
    public void logNewTrade(Long chatId, String symbol, double qty, double entryPrice,
                             double tpPrice, double slPrice) {
        TradeLog trade = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(entryPrice)
                .quantity(qty)
                .takeProfitPrice(BigDecimal.valueOf(tpPrice))
                .stopLossPrice(BigDecimal.valueOf(slPrice))
                .isClosed(false)
                .build();
        tradeLogRepository.save(trade);
    }

    /**
     * Закрываем сделку (по выходу)
     */
    public void closeTrade(Long chatId, String symbol, double exitPrice) {
        Optional<TradeLog> tradeOpt = tradeLogRepository.findFirstByUserChatIdAndSymbolAndIsClosedFalseOrderByEntryTimeDesc(chatId, symbol);
        tradeOpt.ifPresent(trade -> {
            trade.setExitTime(Instant.now());
            trade.setExitPrice(exitPrice);
            trade.setPnl((exitPrice - trade.getEntryPrice()) * trade.getQuantity());
            trade.setIsClosed(true);
            tradeLogRepository.save(trade);
        });
    }

    /**
     * Возвращаем все открытые позиции пользователя
     */
    public List<TradeLog> findOpenTrades(Long chatId) {
        return tradeLogRepository.findAllByUserChatIdAndIsClosedFalse(chatId);
    }

    /**
     * Возвращаем все открытые позиции по конкретному символу
     */
    public List<TradeLog> findOpenTradesBySymbol(Long chatId, String symbol) {
        return tradeLogRepository.findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
    }
}
