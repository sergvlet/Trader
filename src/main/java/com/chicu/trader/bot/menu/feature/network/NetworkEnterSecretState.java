// src/main/java/com/chicu/trader/bot/menu/feature/network/NetworkEnterSecretState.java
package com.chicu.trader.bot.menu.feature.network;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.UserSettingsService;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class NetworkEnterSecretState implements MenuState {

    private final UserSettingsService settings;

    public NetworkEnterSecretState(UserSettingsService settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "network_enter_secret";
    }

    @Override
    public SendMessage render(Long chatId) {
        String prompt = "🔐 Пожалуйста, введите секретный ключ для биржи "
                + settings.getExchange(chatId)
                + " в режиме " + settings.getMode(chatId) + ":";
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(prompt)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return name();
        }

        Long chatId = update.getMessage().getChatId();
        String secret = update.getMessage().getText().trim();
        settings.setSecretKey(chatId, secret);

        // сразу возвращаем на экран настроек,
        // а NetworkSettingsState.render покажет статус соединения
        return MenuService.STATE_NETWORK_SETTINGS;
    }
}
