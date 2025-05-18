// src/main/java/com/chicu/trader/bot/service/MenuEditor.java
package com.chicu.trader.bot.service;

import com.chicu.trader.bot.menu.core.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Service
@RequiredArgsConstructor
public class MenuEditor {

    private final MenuService menuService;
    private final MenuSessionService sessionService;
    // Используем ObjectProvider вместо прямого TelegramSender
    private final ObjectProvider<TelegramSender> telegramSenderProvider;

    public void updateMenu(Long chatId, String stateName) {
        Integer messageId = sessionService.getMenuMessageId(chatId);
        if (messageId == null) {
            return;
        }

        SendMessage send = menuService.renderState(stateName, chatId);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(send.getText());
        edit.setParseMode(send.getParseMode());
        edit.setReplyMarkup((InlineKeyboardMarkup) send.getReplyMarkup());

        // Получаем TelegramSender только при отправке
        TelegramSender sender = telegramSenderProvider.getIfAvailable();
        if (sender != null) {
            sender.executeEdit(edit);
        }
    }
}
