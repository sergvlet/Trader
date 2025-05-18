// src/main/java/com/chicu/trader/bot/service/TelegramSender.java
package com.chicu.trader.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Service
@RequiredArgsConstructor
public class TelegramSender {

    // Инъектим бота лениным образом
    private final @Lazy AbsSender bot;

    /** Обычная отправка SendMessage */
    public void executeUnchecked(SendMessage msg) {
        try {
            bot.execute(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /** Редактирование, игнорируя "message is not modified" */
    public void executeEdit(EditMessageText edit) {
        try {
            bot.execute(edit);
        } catch (TelegramApiRequestException e) {
            String resp = e.getApiResponse();
            if (resp != null && resp.contains("message is not modified")) {
                return;
            }
            throw new RuntimeException("Failed to edit message", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to edit message", e);
        }
    }
}
