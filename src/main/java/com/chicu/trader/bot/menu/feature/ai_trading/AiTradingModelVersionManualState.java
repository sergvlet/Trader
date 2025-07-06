// AiTradingModelVersionManualState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class AiTradingModelVersionManualState implements MenuState {

    private final AiTradingSettingsService svc;

    public AiTradingModelVersionManualState(AiTradingSettingsService svc) {
        this.svc = svc;
    }

    @Override public String name() { return "ai_trading_settings_model_version_manual"; }

    @Override
    public SendMessage render(Long chatId) {
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text("✏️ *Укажите версию ML-модели*\nНапример: `v1`, `v2-beta`")
            .parseMode("Markdown")
            .build();
    }

    @Override
    public @NonNull String handleInput(Update u) {
        if (u.hasMessage() && u.getMessage().hasText()) {
            Long cid = u.getMessage().getChatId();
            String txt = u.getMessage().getText().trim();
            svc.updateModelVersion(cid, txt);
            return "ai_trading_settings";
        }
        return name();
    }
}
