// src/main/java/com/chicu/trader/bot/menu/feature/network/NetworkSelectModeState.java
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
public class NetworkSelectModeState implements MenuState {

    private final UserSettingsService settings;

    public NetworkSelectModeState(UserSettingsService settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "network_select_mode";
    }

    @Override
    public SendMessage render(Long chatId) {
        InlineKeyboardButton testBtn = InlineKeyboardButton.builder()
            .text("🧪 Тестовый режим" + (!"REAL".equalsIgnoreCase(settings.getMode(chatId)) ? " ✅" : ""))
            .callbackData("mode_test")
            .build();
        InlineKeyboardButton realBtn = InlineKeyboardButton.builder()
            .text("💰 Реальный режим" + ("REAL".equalsIgnoreCase(settings.getMode(chatId)) ? " ✅" : ""))
            .callbackData("mode_real")
            .build();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData(MenuService.BACK)
            .build();

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(testBtn),
                List.of(realBtn),
                List.of(backBtn)
            ))
            .build();

        return SendMessage.builder()
            .chatId(chatId.toString())
            .text("Выберите торговый режим:")
            .replyMarkup(kb)
            .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (data) {
            case "mode_test":
                settings.setMode(chatId, "TEST");
                break;
            case "mode_real":
                settings.setMode(chatId, "REAL");
                break;
            case MenuService.BACK:
                return MenuService.BACK;
            default:
                return name();
        }
        // после установки режима возвращаемся в сетевые настройки
        return MenuService.STATE_NETWORK_SETTINGS;
    }
}
