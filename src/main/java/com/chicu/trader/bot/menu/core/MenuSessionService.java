package com.chicu.trader.bot.menu.core;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MenuSessionService {

    public final Map<Long, Integer> menuMessages = new ConcurrentHashMap<>();

    /** Возвращает messageId, под которым было отправлено меню этому пользователю */
    public Integer getMenuMessageId(Long chatId) {
        return menuMessages.get(chatId);
    }



}
