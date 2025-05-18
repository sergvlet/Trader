// src/main/java/com/chicu/trader/trading/TradeLogPositionService.java
package com.chicu.trader.trading;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeLogPositionService implements PositionService {

    private final TradeLogRepository logRepo;

    @Override
    public int getActiveSlots(Long chatId) {
        List<TradeLog> opens = logRepo.findByUserChatIdAndIsClosedFalse(chatId);
        return opens.size();
    }

    @Override
    public double getOpenPositionQuantity(Long chatId, String symbol) {
        return logRepo.findByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol)
                      .stream()
                      .mapToDouble(TradeLog::getQuantity)
                      .sum();
    }
}
