package com.chicu.trader.bot.menu.core;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Интерфейс экрана-меню.
 */
public interface MenuState {
    /** Уникальное имя состояния (должно совпадать с возвращаемым handleInput). */
    String name();

    /** Сгенерировать сообщение для отображения этого экрана. */
    SendMessage render(Long chatId);

    /**
     * Обработать апдейт (callback или сообщение).
     * @return имя следующего состояния, или "BACK" — чтобы вернуться назад,
     * или своё name() — чтобы остаться.
     */
    String handleInput(Update update);
}
