// src/main/java/com/chicu/trader/bot/service/MenuSessionService.java
package com.chicu.trader.bot.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class MenuSessionService {
    private final ConcurrentMap<Long, Integer> lastMenuMessage = new ConcurrentHashMap<>();

    /** Запоминаем messageId меню для chatId */
    public void updateMenuMessage(Long chatId, Integer messageId) {
        lastMenuMessage.put(chatId, messageId);
    }

    /** Получаем последнее messageId меню для chatId (или null) */
    public Integer getMenuMessageId(Long chatId) {
        return lastMenuMessage.get(chatId);
    }
}
