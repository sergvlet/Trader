package com.chicu.trader.trading.repository;

import com.chicu.trader.model.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    /**
     * Список всех сделок пользователя.
     */
    List<TradeLog> findAllByUserChatId(Long chatId);

    /**
     * Все незакрытые сделки пользователя по конкретному символу.
     */
    List<TradeLog> findAllByUserChatIdAndSymbolAndIsClosedFalse(
            Long chatId,
            String symbol
    );
    Optional<TradeLog> findTopByUserChatIdOrderByEntryTimeDesc(Long chatId);

    // Проверка на наличие открытой сделки для chatId и символа
    boolean existsByUserChatIdAndSymbolAndIsClosedFalse(Long userChatId, String symbol);


    List<TradeLog> findAllByIsClosedFalse();

}
