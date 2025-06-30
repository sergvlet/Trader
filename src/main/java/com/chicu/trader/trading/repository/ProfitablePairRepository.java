package com.chicu.trader.trading.repository;

import com.chicu.trader.trading.entity.ProfitablePair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfitablePairRepository extends JpaRepository<ProfitablePair, Long> {

    /**
     * Все пары пользователя
     */
    List<ProfitablePair> findByUserChatId(Long chatId);

    /**
     * Только активные пары пользователя
     */
    List<ProfitablePair> findByUserChatIdAndActiveTrue(Long chatId);

    /**
     * Все пары по символу у пользователя
     */
    List<ProfitablePair> findByUserChatIdAndSymbol(Long chatId, String symbol);
}
