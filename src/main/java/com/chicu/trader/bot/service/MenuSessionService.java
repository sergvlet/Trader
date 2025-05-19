package com.chicu.trader.bot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MenuSessionService {
    // хранит для каждого chatId текущее messageId меню
    private final ConcurrentHashMap<Long, Integer> menuMessages = new ConcurrentHashMap<>();

    /** Возвращает messageId, под которым было отправлено меню этому пользователю */
    public Integer getMenuMessageId(Long chatId) {
        return menuMessages.get(chatId);
    }

    /** Удаляет notice-уведомление для пользователя (если нужно) */
    public Optional<BotApiMethod<?>> popNotice(Long chatId) {
        // ваша реализация...
        return Optional.empty();
    }

    // >>>>> ДОБАВЛЕНО ДЛЯ ТЕСТА <<<<<<
    /**
     * Регистрирует в службу, что меню этого пользователя было отправлено
     * и находится под этим messageId.
     * (Используется в интеграционных тестах.)
     */
    public void createMenuMessage(Long chatId, int messageId) {
        menuMessages.put(chatId, messageId);
    }
}
