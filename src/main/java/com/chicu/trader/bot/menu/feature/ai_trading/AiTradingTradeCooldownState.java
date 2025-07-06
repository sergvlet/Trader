// AiTradingTradeCooldownState.java
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
public class AiTradingTradeCooldownState implements MenuState {

    private final AiTradingSettingsService svc;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup kb;

    public AiTradingTradeCooldownState(AiTradingSettingsService svc, AiTradingDefaults defaults) {
        this.svc = svc;
        this.defaults = defaults;
        InlineKeyboardButton inc = InlineKeyboardButton.builder()
                .text("➕ Минуту").callbackData("cooldown_inc").build();
        InlineKeyboardButton dec = InlineKeyboardButton.builder()
                .text("➖ Минуту").callbackData("cooldown_dec").build();
        InlineKeyboardButton def = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию").callbackData("cooldown_default").build();
        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text("💾 Сохранить").callbackData("cooldown_save").build();
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

    @Override public String name() { return "ai_trading_settings_trade_cooldown"; }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = svc.getOrCreate(chatId);
        int val = s.getTradeCooldown() != null 
            ? s.getTradeCooldown() : defaults.getDefaultTradeCooldown();
        String text = String.format(
            "*Задержка между сделками*\nТекущее: `%d` мин", val
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
        AiTradingSettings s = svc.getOrCreate(cid);
        int val = s.getTradeCooldown() != null 
            ? s.getTradeCooldown() : defaults.getDefaultTradeCooldown();

        switch (data) {
            case "cooldown_inc"     -> val++;
            case "cooldown_dec"     -> val = Math.max(0, val - 1);
            case "cooldown_default" -> { svc.resetTradeCooldownDefaults(cid); return name(); }
            case "cooldown_save"    -> { svc.updateTradeCooldown(cid, val); return "ai_trading_settings"; }
            case "ai_trading_settings" -> { return "ai_trading_settings"; }
            default                 -> { return name(); }
        }
        svc.updateTradeCooldown(cid, val);
        return name();
    }
}
