// AiTradingModelVersionState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AiTradingModelVersionState implements MenuState {

    @Override public String name() { return "ai_trading_settings_model_version"; }

    private final AiTradingSettingsService svc;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup kb;

    public AiTradingModelVersionState(AiTradingSettingsService svc, AiTradingDefaults defaults) {
        this.svc = svc;
        this.defaults = defaults;
        InlineKeyboardButton edit  = InlineKeyboardButton.builder()
                .text("✏️ Изменить").callbackData("model_ver_edit").build();
        InlineKeyboardButton def   = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию").callbackData("model_ver_default").build();
        InlineKeyboardButton back  = InlineKeyboardButton.builder()
                .text("‹ Назад").callbackData("ai_trading_settings").build();
        this.kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(edit),
                List.of(def),
                List.of(back)
            )).build();
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = svc.getOrCreate(chatId);
        String cur = s.getModelVersion() != null 
            ? s.getModelVersion() : defaults.getDefaultModelVersion();
        String text = String.format(
            "*Версия ML-модели*\nТекущая: `%s`\n\nВыберите действие:", cur
        );
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text).parseMode("Markdown")
                .replyMarkup(kb).build();
    }

    @Override
    public @NonNull String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();
        String data = u.getCallbackQuery().getData();
        Long cid = u.getCallbackQuery().getMessage().getChatId();
        return switch (data) {
            case "model_ver_edit"    -> "ai_trading_settings_model_version_manual";
            case "model_ver_default" -> { svc.resetModelVersionDefaults(cid); yield name(); }
            case "ai_trading_settings" -> "ai_trading_settings";
            default                  -> name();
        };
    }
}
