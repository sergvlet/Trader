package com.chicu.trader.bot.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class AiTradingService {
    // Храним, включён ли AI-режим для каждого chatId
    private final Map<Long, Boolean> enabledMap = new ConcurrentHashMap<>();

    /** Возвращает текущее состояние AI-режима */
    public boolean isEnabled(Long chatId) {
        return enabledMap.getOrDefault(chatId, false);
    }

    /** Переключает состояние и возвращает новое значение */
    public boolean toggle(Long chatId) {
        boolean newVal = !isEnabled(chatId);
        enabledMap.put(chatId, newVal);
        return newVal;
    }
}
