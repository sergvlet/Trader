// src/main/java/com/chicu/trader/bot/menu/feature/network/NetworkSettingsState.java
package com.chicu.trader.bot.menu.feature.network;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.UserSettingsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class NetworkSettingsState implements MenuState {

    private final UserSettingsService settings;

    public NetworkSettingsState(UserSettingsService settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return MenuService.STATE_NETWORK_SETTINGS;
    }

    @Override
    public SendMessage render(Long chatId) {
        String exch = settings.getExchange(chatId);
        String mode = settings.getMode(chatId);
        boolean hasCred = settings.hasCredentials(chatId);
        boolean ok = settings.testConnection(chatId);

        String exchText = exch != null ? exch : "не задана";
        String modeText = mode != null ? mode : "не задан";

        InlineKeyboardButton exchBtn = InlineKeyboardButton.builder()
                .text("🌐 Биржа: " + exchText)
                .callbackData("network_select_exchange")
                .build();
        InlineKeyboardButton modeBtn = InlineKeyboardButton.builder()
                .text("🧪 Режим: " + modeText)
                .callbackData("network_select_mode")
                .build();
        InlineKeyboardButton keysBtn = InlineKeyboardButton.builder()
                .text(exch != null
                        ? (hasCred ? "🔑 Изменить ключи" : "🔑 Ввести ключи")
                        : "🔑 Сначала выберите биржу")
                .callbackData(exch != null
                        ? "network_enter_api"
                        : "network_select_exchange")
                .build();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_settings")
                .build();

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(exchBtn),
                        List.of(modeBtn),
                        List.of(keysBtn),
                        List.of(backBtn)
                ))
                .build();

        String connectionLine = hasCred
                ? (ok ? "✅ Соединение установлено" : "❌ Ошибка соединения")
                : "";

        String text = "*Сетевые настройки*\n" +
                "Биржа: " + exchText + "\n" +
                "Режим: " + modeText + "\n" +
                "Ключи: " + (hasCred ? "заданы" : "не заданы") +
                (connectionLine.isEmpty() ? "" : "\n" + connectionLine) +
                "\n\nВыберите действие:";
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();
        String data = update.getCallbackQuery().getData();

        // возвращаем в меню AI-настроек
        if ("ai_trading_settings".equals(data)) {
            return "ai_trading_settings";
        }

        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        return switch (data) {
            case "network_select_exchange" -> "network_select_exchange";
            case "network_select_mode"     -> "network_select_mode";
            case "network_enter_api"       -> {
                if (settings.getExchange(chatId) == null) {
                    yield "network_select_exchange";
                }
                yield "network_enter_api";
            }
            default -> name();
        };
    }
}
