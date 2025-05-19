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
public class AiTradingTimeframeState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup keyboard;

    public AiTradingTimeframeState(AiTradingSettingsService settingsService,
                                   AiTradingDefaults defaults) {
        this.settingsService = settingsService;
        this.defaults = defaults;

        InlineKeyboardButton b1m  = InlineKeyboardButton.builder()
                .text("1m").callbackData("timeframe_1m").build();
        InlineKeyboardButton b5m  = InlineKeyboardButton.builder()
                .text("5m").callbackData("timeframe_5m").build();
        InlineKeyboardButton b15m = InlineKeyboardButton.builder()
                .text("15m").callbackData("timeframe_15m").build();

        InlineKeyboardButton b1h  = InlineKeyboardButton.builder()
                .text("1h").callbackData("timeframe_1h").build();
        InlineKeyboardButton b4h  = InlineKeyboardButton.builder()
                .text("4h").callbackData("timeframe_4h").build();
        InlineKeyboardButton b1d  = InlineKeyboardButton.builder()
                .text("1d").callbackData("timeframe_1d").build();

        InlineKeyboardButton def   = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию")
                .callbackData("timeframe_default")
                .build();
        InlineKeyboardButton save  = InlineKeyboardButton.builder()
                .text("💾 Сохранить")
                .callbackData("timeframe_save")
                .build();
        InlineKeyboardButton back  = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("timeframe_back")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                    List.of(b1m,  b5m,  b15m),
                    List.of(b1h,  b4h,  b1d),
                    List.of(def),
                    List.of(save),
                    List.of(back)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings_timeframe";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        String current = s.getTimeframe() != null
                ? s.getTimeframe()
                : defaults.getDefaultTimeframe();
        String text = String.format("*Таймфрейм*\nТекущий: `%s`", current);

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
        // берём текущее или дефолт
        String tf = settingsService.getOrCreate(chatId).getTimeframe();
        if (tf == null) tf = defaults.getDefaultTimeframe();

        switch (data) {
            case "timeframe_1m"      -> tf = "1m";
            case "timeframe_5m"      -> tf = "5m";
            case "timeframe_15m"     -> tf = "15m";
            case "timeframe_1h"      -> tf = "1h";
            case "timeframe_4h"      -> tf = "4h";
            case "timeframe_1d"      -> tf = "1d";
            case "timeframe_default" -> {
                settingsService.resetTimeframeDefaults(chatId);
                return name();
            }
            case "timeframe_save"    -> {
                settingsService.updateTimeframe(chatId, tf);
                return "ai_trading_settings";
            }
            case "timeframe_back"    -> {
                return "ai_trading_settings";
            }
            default                  -> {
                return name();
            }
        }
        // при выборе сразу сохраняем и перерисовываем
        settingsService.updateTimeframe(chatId, tf);
        return name();
    }
}
