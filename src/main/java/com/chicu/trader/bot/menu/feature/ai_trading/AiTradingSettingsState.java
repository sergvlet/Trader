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
 * Подменю «⚙️ Настройки» AI-режима:
 *  - Сетевые настройки
 *  - TP/SL настройки
 *  - Режим реинвестирования
 *  - Назад
 */
@Component
public class AiTradingSettingsState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public AiTradingSettingsState() {
        InlineKeyboardButton networkBtn = InlineKeyboardButton.builder()
            .text("🌐 Сетевые настройки")
            .callbackData("network_settings")
            .build();

        InlineKeyboardButton tpSlBtn = InlineKeyboardButton.builder()
            .text("📈 TP/SL настройки")
            .callbackData("ai_trading_settings_tp_sl")
            .build();

        InlineKeyboardButton reinvestBtn = InlineKeyboardButton.builder()
            .text("🔄 Режим реинвестирования")
            .callbackData("ai_trading_settings_reinvest")
            .build();

        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData("ai_trading")
            .build();

        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(networkBtn),
                List.of(tpSlBtn),
                List.of(reinvestBtn),
                List.of(backBtn)
            ))
            .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings";
    }

    @Override
    public SendMessage render(Long chatId) {
        String text = "*Настройки AI-торговли*\nВыберите пункт для изменения:";
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
            case "network_settings":
                return "network_settings";
            case "ai_trading_settings_tp_sl":
                return "ai_trading_settings_tp_sl";
            case "ai_trading_settings_reinvest":
                return "ai_trading_settings_reinvest";
            case "ai_trading":
                return MenuService.BACK;
            default:
                return name();
        }
    }
}
