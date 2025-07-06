package com.chicu.trader.bot.menu.feature.about;

import com.chicu.trader.bot.menu.core.MenuState;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AboutState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public AboutState() {
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData("MAIN_MENU")
            .build();
        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(List.of(backBtn)))
            .build();
    }

    @Override
    public String name() {
        return "about";
    }

    @Override
    public SendMessage render(Long chatId) {
        String text = "*О боте*\n" +
            "Этот торговый бот умеет работать на разных биржах в двух режимах:\n\n" +
            "🤚 *Ручная торговля* — вы сами выставляете ордера через бота;\n" +
            "🤖 *AI-режим* — бот торгует автоматически по стратегии RSI-BB + ML-фильтр,\n" +
            "   ежедневно оптимизирует TP/SL и переобучает модель.\n\n" +
            "Выберите раздел в меню.";
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        if (update.hasCallbackQuery()
         && "MAIN_MENU".equals(update.getCallbackQuery().getData())) {
            return "MAIN_MENU";
        }
        return name();
    }
}
