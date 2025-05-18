// src/main/java/com/chicu/trader/bot/menu/feature/network/NetworkSelectExchangeState.java
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
public class NetworkSelectExchangeState implements MenuState {

    private final UserSettingsService settings;

    public NetworkSelectExchangeState(UserSettingsService settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "network_select_exchange";
    }

    @Override
    public SendMessage render(Long chatId) {
        InlineKeyboardButton binance = InlineKeyboardButton.builder()
            .text("Binance")
            .callbackData("network_select:Binance")
            .build();
        InlineKeyboardButton kucoin = InlineKeyboardButton.builder()
            .text("KuCoin")
            .callbackData("network_select:KuCoin")
            .build();
        InlineKeyboardButton bybit = InlineKeyboardButton.builder()
            .text("Bybit")
            .callbackData("network_select:Bybit")
            .build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData(MenuService.STATE_NETWORK_SETTINGS)
            .build();

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(binance),
                List.of(kucoin),
                List.of(bybit),
                List.of(back)
            ))
            .build();

        String text = "*Выберите биржу*";
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
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

        if (data.startsWith("network_select:")) {
            String exch = data.substring("network_select:".length());
            settings.setExchange(chatId, exch);
            return MenuService.STATE_NETWORK_SETTINGS;
        }
        if (MenuService.STATE_NETWORK_SETTINGS.equals(data)) {
            return MenuService.STATE_NETWORK_SETTINGS;
        }
        return name();
    }
}
