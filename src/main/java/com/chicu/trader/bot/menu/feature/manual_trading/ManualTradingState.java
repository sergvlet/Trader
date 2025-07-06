package com.chicu.trader.bot.menu.feature.manual_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Экран «Ручная торговля» — точка входа в ручной интерфейс.
 */
@Component
public class ManualTradingState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public ManualTradingState() {
        InlineKeyboardButton settingsBtn = InlineKeyboardButton.builder()
            .text("⚙️ Настройки")
            .callbackData("manual_trading:settings")
            .build();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData("MAIN_MENU")
            .build();

        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(settingsBtn),
                List.of(backBtn)
            ))
            .build();
    }

    @Override
    public String name() {
        return "manual_trading";
    }

    @Override
    public SendMessage render(Long chatId) {
        String text = "*Ручная торговля*\n" +
                      "Здесь вы сможете выставлять ордера вручную.\n" +
                      "Нажмите «Настройки», чтобы выбрать биржу и указать ключи.";
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data = update.getCallbackQuery().getData();
        switch (data) {
            case "manual_trading:settings":
                return "manual_trading_settings";
            case "MAIN_MENU":
                return MenuService.BACK;
            default:
                return name();
        }
    }
}
