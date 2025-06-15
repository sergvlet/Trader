package com.chicu.trader.trading.repository;

import com.chicu.trader.trading.entity.ProfitablePair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfitablePairRepository extends JpaRepository<ProfitablePair, Long> {

    List<ProfitablePair> findByUserChatId(Long chatId);

    // ДОБАВЛЯЕМ ЭТОТ МЕТОД
    List<ProfitablePair> findByUserChatIdAndActiveTrue(Long chatId);

    Optional<ProfitablePair> findByUserChatIdAndSymbol(Long chatId, String symbol);
}
