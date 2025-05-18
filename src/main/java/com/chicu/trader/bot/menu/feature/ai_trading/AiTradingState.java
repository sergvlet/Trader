package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Вход в AI-раздел: выбор подменю
 */
@Component
public class AiTradingState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public AiTradingState() {
        InlineKeyboardButton settingsBtn = InlineKeyboardButton.builder()
            .text("⚙️ Настройки")
            .callbackData("ai_trading:settings")
            .build();
        InlineKeyboardButton statsBtn = InlineKeyboardButton.builder()
            .text("📊 Статистика")
            .callbackData("ai_trading:statistics")
            .build();
        InlineKeyboardButton ordersBtn = InlineKeyboardButton.builder()
            .text("📋 Ордера")
            .callbackData("ai_trading:orders")
            .build();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData(MenuService.BACK)
            .build();

        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(settingsBtn, statsBtn),
                List.of(ordersBtn),
                List.of(backBtn)
            ))
            .build();
    }

    @Override
    public String name() {
        return "ai_trading";
    }

    @Override
    public SendMessage render(Long chatId) {
        String text = "*AI-торговля*\nВыберите действие в AI-режиме:";
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data = update.getCallbackQuery().getData();
        switch (data) {
            case "ai_trading:settings":
                return "ai_trading_settings";
            case "ai_trading:statistics":
                return "ai_trading_statistics";
            case "ai_trading:orders":
                return "ai_trading_orders";
            case MenuService.BACK:
                return MenuService.BACK;
            default:
                return name();
        }
    }
}
