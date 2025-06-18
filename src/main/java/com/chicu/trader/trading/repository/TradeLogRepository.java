package com.chicu.trader.trading.repository;

import com.chicu.trader.trading.entity.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    // Проверить, есть ли открытая сделка по чату и символу
    boolean existsByUserChatIdAndSymbolAndClosedFalse(Long chatId, String symbol);

    // Все открытые сделки по чату и символу
    List<TradeLog> findAllByUserChatIdAndSymbolAndClosedFalse(Long chatId, String symbol);

    // Все открытые сделки
    List<TradeLog> findAllByClosedFalse();

    // Недавние закрытые прибыльные сделки для пользователя
    @Query("""
        SELECT t
          FROM TradeLog t
         WHERE t.userChatId = :chatId
           AND t.closed     = true
           AND t.pnl        > 0
         ORDER BY t.exitTime DESC
        """)
    List<TradeLog> findRecentClosedProfitableTrades(@Param("chatId") Long chatId);

    // Последняя открытая сделка по символу
    Optional<TradeLog> findFirstByUserChatIdAndSymbolAndClosedFalseOrderByEntryTimeDesc(
            Long chatId,
            String symbol
    );

    // Все открытые сделки для пользователя
    List<TradeLog> findAllByUserChatIdAndClosedFalse(Long chatId);

    /**
     * Найти открытую сделку по идентификатору клиентского ордера на входе.
     */
    @Query("""
        SELECT t
          FROM TradeLog t
         WHERE t.entryClientOrderId = :cid
           AND t.closed             = false
        """)
    Optional<TradeLog> findOpenByEntryClientOrderId(@Param("cid") String entryClientOrderId);
}
