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
public class AiTradingRiskState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup keyboard;

    public AiTradingRiskState(AiTradingSettingsService settingsService,
                              AiTradingDefaults defaults) {
        this.settingsService = settingsService;
        this.defaults = defaults;

        InlineKeyboardButton inc  = InlineKeyboardButton.builder()
                .text("➕ % баланса")
                .callbackData("risk_inc")
                .build();
        InlineKeyboardButton dec  = InlineKeyboardButton.builder()
                .text("➖ % баланса")
                .callbackData("risk_dec")
                .build();
        InlineKeyboardButton def  = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию")
                .callbackData("risk_default")
                .build();
        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text("💾 Сохранить")
                .callbackData("risk_save")
                .build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("risk_back")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                    List.of(inc, dec),
                    List.of(def),
                    List.of(save),
                    List.of(back)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings_risk";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        // если не задано — берем дефолт
        double risk = (s.getRiskThreshold() != null)
                ? s.getRiskThreshold()
                : defaults.getDefaultRiskThreshold();
        String text = String.format(
            "*Риски*\nПроцент баланса на сделку: `%.1f%%`", risk
        );
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();
        // текущее или дефолт
        Double risk = settingsService.getOrCreate(chatId).getRiskThreshold();
        if (risk == null) {
            risk = defaults.getDefaultRiskThreshold();
        }

        switch (data) {
            case "risk_inc"       -> risk = risk + 1.0;
            case "risk_dec"       -> risk = Math.max(0.1, risk - 1.0);
            case "risk_default"   -> {
                settingsService.resetRiskThresholdDefaults(chatId);
                return name();
            }
            case "risk_save"      -> {
                settingsService.updateRiskThreshold(chatId, risk);
                return "ai_trading_settings";
            }
            case "risk_back"      -> {
                return "ai_trading_settings";
            }
            default               -> {
                return name();
            }
        }
        // при инкременте/декременте сразу сохраняем и перерисовываем
        settingsService.updateRiskThreshold(chatId, risk);
        return name();
    }
}
