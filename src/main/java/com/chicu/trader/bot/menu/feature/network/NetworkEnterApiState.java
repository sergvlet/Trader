// src/main/java/com/chicu/trader/bot/menu/feature/network/NetworkEnterApiState.java
package com.chicu.trader.bot.menu.feature.network;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class NetworkEnterApiState implements MenuState {

    private final UserSettingsService settings;

    @Override
    public String name() {
        return "network_enter_api";
    }

    @Override
    public SendMessage render(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("🔑 Введите API Key для режима " + settings.getMode(chatId))
                .build();
    }
    @Override
    public String handleInput(Update upd) {
        if (upd.hasMessage() && upd.getMessage().hasText()) {
            Long id = upd.getMessage().getChatId();
            settings.setApiKey(id, upd.getMessage().getText().trim());
            return "network_enter_secret";
        }
        return name();
    }
}