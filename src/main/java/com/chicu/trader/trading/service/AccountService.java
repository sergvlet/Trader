// src/main/java/com/chicu/trader/trading/service/AccountService.java
package com.chicu.trader.trading.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис работы с балансом пользователя на бирже.
 */
@Service
@Slf4j
public class AccountService {

    /**
     * Возвращает свободный баланс базового актива (например, USD) для данного пользователя.
     *
     * @param chatId  чат пользователя
     * @param asset   базовый актив (например, "USD")
     * @return доступный баланс
     */
    public double getFreeBalance(Long chatId, String asset) {
        log.debug("Получение баланса для chatId={} asset={}", chatId, asset);
        // TODO: запросить баланс через API биржи или из БД
        return 0.0;
    }
}
