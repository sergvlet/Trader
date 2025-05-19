// AiTradingSlippageToleranceState.java
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
public class AiTradingSlippageToleranceState implements MenuState {

    private final AiTradingSettingsService svc;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup kb;

    public AiTradingSlippageToleranceState(AiTradingSettingsService svc, AiTradingDefaults defaults) {
        this.svc = svc;
        this.defaults = defaults;
        InlineKeyboardButton inc = InlineKeyboardButton.builder()
                .text("➕ %").callbackData("slippage_inc").build();
        InlineKeyboardButton dec = InlineKeyboardButton.builder()
                .text("➖ %").callbackData("slippage_dec").build();
        InlineKeyboardButton def = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию").callbackData("slippage_default").build();
        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text("💾 Сохранить").callbackData("slippage_save").build();
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

    @Override public String name() { return "ai_trading_settings_slippage_tolerance"; }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = svc.getOrCreate(chatId);
        double val = s.getSlippageTolerance() != null 
            ? s.getSlippageTolerance() : defaults.getDefaultSlippageTolerance();
        String text = String.format(
            "*Проскальзывание*\nТекущее: `%.1f%%`", val
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
        double val = s.getSlippageTolerance() != null 
            ? s.getSlippageTolerance() : defaults.getDefaultSlippageTolerance();

        switch (data) {
            case "slippage_inc"     -> val += 0.1;
            case "slippage_dec"     -> val = Math.max(0.0, val - 0.1);
            case "slippage_default" -> { svc.resetSlippageToleranceDefaults(cid); return name(); }
            case "slippage_save"    -> { svc.updateSlippageTolerance(cid, val); return "ai_trading_settings"; }
            case "ai_trading_settings" -> { return "ai_trading_settings"; }
            default                 -> { return name(); }
        }
        svc.updateSlippageTolerance(cid, val);
        return name();
    }
}
