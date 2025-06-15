package com.chicu.trader.trading.repository;

import com.chicu.trader.trading.entity.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    boolean existsByUserChatIdAndSymbolAndIsClosedFalse(Long chatId, String symbol);

    List<TradeLog> findAllByUserChatIdAndSymbolAndIsClosedFalse(Long chatId, String symbol);

    List<TradeLog> findAllByIsClosedFalse();

    @Query("""
        SELECT t FROM TradeLog t
        WHERE t.userChatId = :chatId AND t.isClosed = true AND t.pnl > 0
        ORDER BY t.exitTime DESC
        """)
    List<TradeLog> findRecentClosedProfitableTrades(Long chatId);

    // Метод для поиска последней открытой сделки по символу
    Optional<TradeLog> findFirstByUserChatIdAndSymbolAndIsClosedFalseOrderByEntryTimeDesc(Long chatId, String symbol);

    // Метод для поиска всех открытых сделок по пользователю
    List<TradeLog> findAllByUserChatIdAndIsClosedFalse(Long chatId);
}

