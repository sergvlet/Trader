package com.chicu.trader.trading.repository;

import com.chicu.trader.model.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
    List<TradeLog> findAllByUserChatId(Long chatId);
}
