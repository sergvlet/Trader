package com.chicu.trader.bot.menu.feature.manual_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Подменю «⚙️ Настройки» ручной торговли:
 *  - Сетевые настройки (универсально)
 *  - ‹ Назад
 */
@Component
public class ManualTradingSettingsState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public ManualTradingSettingsState() {
        InlineKeyboardButton networkBtn = InlineKeyboardButton.builder()
            .text("🌐 Сетевые настройки")
            .callbackData("network_settings")
            .build();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData("manual_trading")
            .build();

        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(networkBtn),
                List.of(backBtn)
            ))
            .build();
    }

    @Override
    public String name() {
        return "manual_trading_settings";
    }

    @Override
    public SendMessage render(Long chatId) {
        String text = "*Настройки ручной торговли*\n" +
                      "Здесь можно выбрать биржу, режим и ввести ключи для ручных ордеров.";
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();
        String data = update.getCallbackQuery().getData();
        switch (data) {
            case "network_settings":
                return "network_settings";
            case "manual_trading":
                return MenuService.BACK;
            default:
                return name();
        }
    }
}
