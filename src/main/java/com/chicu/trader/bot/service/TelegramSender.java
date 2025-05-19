// src/main/java/com/chicu/trader/bot/service/TelegramSender.java
package com.chicu.trader.bot.service;

import com.chicu.trader.bot.TraderTelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramSender {

    private final TraderTelegramBot bot;

    /**
     * Редактирует уже отправленное меню. При ошибке просто логируем.
     */
    public void executeEdit(EditMessageText edit) {
        try {
            bot.execute(edit);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отредактировать меню: {}", e.getMessage());
        }
    }

    /**
     * Отправляет простое текстовое сообщение. При ошибке — лог и продолжаем.
     */
    public void sendText(Long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить текст: {}", e.getMessage());
        }
    }

}
