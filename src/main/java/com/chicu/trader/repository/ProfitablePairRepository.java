// src/main/java/com/chicu/trader/repository/ProfitablePairRepository.java
package com.chicu.trader.repository;

import com.chicu.trader.model.ProfitablePair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProfitablePairRepository extends JpaRepository<ProfitablePair, Long> {


    /**
     * Все пары (включённые и выключенные) для данного пользователя.
     */
    List<ProfitablePair> findByUserChatId(Long userChatId);

    /**
     * Только активные пары для данного пользователя.
     */
    List<ProfitablePair> findByUserChatIdAndActiveTrue(Long userChatId);

    /**
     * Список всех chatId, для которых есть хотя бы одна пара.
     */
    @Query("SELECT DISTINCT p.userChatId FROM ProfitablePair p")
    List<Long> findAllDistinctChatIds();
}
