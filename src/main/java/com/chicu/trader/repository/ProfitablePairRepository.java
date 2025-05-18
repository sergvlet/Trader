// src/main/java/com/chicu/trader/repository/ProfitablePairRepository.java
package com.chicu.trader.repository;

import com.chicu.trader.model.ProfitablePair;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfitablePairRepository extends JpaRepository<ProfitablePair, Long> {
    List<ProfitablePair> findByUserChatIdAndActiveTrue(Long chatId);
}
