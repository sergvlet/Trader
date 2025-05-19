package com.chicu.trader.bot.menu.core;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MenuSessionService {
    // хранит для каждого chatId текущее messageId меню
    private final Map<Long, Integer> menuMessages = new ConcurrentHashMap<>();

    /** Возвращает messageId, под которым было отправлено меню этому пользователю */
    public Integer getMenuMessageId(Long chatId) {
        return menuMessages.get(chatId);
    }

    /** Удаляет notice-уведомление для пользователя (если нужно) */
    public Optional<BotApiMethod<?>> popNotice(Long chatId) {
        // ваша реализация...
        return Optional.empty();
    }

    /**
     * Регистрирует в службу, что меню этого пользователя было отправлено
     * и находится под этим messageId.
     * (Используется в интеграционных тестах.)
     */
    public void createMenuMessage(Long chatId, int messageId) {
        menuMessages.put(chatId, messageId);
    }

    // Для постраничного вывода списка пар
    private final Map<Long, Integer> pairsPageMap = new ConcurrentHashMap<>();

    /** Получить текущую страницу списка пар */
    public int getPairsPage(Long chatId) {
        return pairsPageMap.getOrDefault(chatId, 0);
    }
    /** Установить страницу списка пар */
    public void setPairsPage(Long chatId, int page) {
        pairsPageMap.put(chatId, page);
    }
}
