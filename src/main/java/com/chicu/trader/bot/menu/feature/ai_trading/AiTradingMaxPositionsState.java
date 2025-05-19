// AiTradingMaxPositionsState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AiTradingMaxPositionsState implements MenuState {

    private final AiTradingSettingsService svc;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup kb;

    public AiTradingMaxPositionsState(AiTradingSettingsService svc, AiTradingDefaults defaults) {
        this.svc = svc;
        this.defaults = defaults;
        InlineKeyboardButton inc = InlineKeyboardButton.builder()
                .text("➕ Позиция").callbackData("maxpos_inc").build();
        InlineKeyboardButton dec = InlineKeyboardButton.builder()
                .text("➖ Позиция").callbackData("maxpos_dec").build();
        InlineKeyboardButton def = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию").callbackData("maxpos_default").build();
        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text("💾 Сохранить").callbackData("maxpos_save").build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("‹ Назад").callbackData("ai_trading_settings").build();
        this.kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(inc, dec),
                List.of(def),
                List.of(save),
                List.of(back)
            )).build();
    }

    @Override public String name() { return "ai_trading_settings_max_positions"; }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = svc.getOrCreate(chatId);
        int val = s.getMaxPositions() != null 
            ? s.getMaxPositions() : defaults.getDefaultMaxPositions();
        String text = String.format(
            "*Макс. позиций*\nТекущее: `%d`", val
        );
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text).parseMode("Markdown")
                .replyMarkup(kb).build();
    }

    @Override
    public String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();
        String data = u.getCallbackQuery().getData();
        Long cid = u.getCallbackQuery().getMessage().getChatId();
        AiTradingSettings s = svc.getOrCreate(cid);
        int val = s.getMaxPositions() != null 
            ? s.getMaxPositions() : defaults.getDefaultMaxPositions();

        switch (data) {
            case "maxpos_inc"     -> val++;
            case "maxpos_dec"     -> val = Math.max(1, val - 1);
            case "maxpos_default" -> { svc.resetMaxPositionsDefaults(cid); return name(); }
            case "maxpos_save"    -> { svc.updateMaxPositions(cid, val); return "ai_trading_settings"; }
            case "ai_trading_settings" -> { return "ai_trading_settings"; }
            default               -> { return name(); }
        }
        svc.updateMaxPositions(cid, val);
        return name();
    }
}
