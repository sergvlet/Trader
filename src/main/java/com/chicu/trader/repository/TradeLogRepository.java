// src/main/java/com/chicu/trader/repository/TradeLogRepository.java
package com.chicu.trader.repository;

import com.chicu.trader.model.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
    List<TradeLog> findByUserChatIdAndIsClosedFalse(Long chatId);
    List<TradeLog> findByUserChatIdAndSymbolAndIsClosedFalse(Long chatId, String symbol);
}
