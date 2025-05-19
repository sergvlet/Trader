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
public class AiTradingTopNState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup keyboard;

    public AiTradingTopNState(AiTradingSettingsService settingsService,
                              AiTradingDefaults defaults) {
        this.settingsService = settingsService;
        this.defaults = defaults;

        InlineKeyboardButton inc  = InlineKeyboardButton.builder()
                .text("➕ Кол-во пар")
                .callbackData("topn_inc")
                .build();
        InlineKeyboardButton dec  = InlineKeyboardButton.builder()
                .text("➖ Кол-во пар")
                .callbackData("topn_dec")
                .build();
        InlineKeyboardButton def  = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию")
                .callbackData("topn_default")
                .build();
        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text("💾 Сохранить")
                .callbackData("topn_save")
                .build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("topn_back")
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
        return "ai_trading_settings_topn";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        int topN = (s.getTopN() != null) ? s.getTopN() : defaults.getDefaultTopN();
        String text = String.format("*Top N*\nТекущее значение: `%d`", topN);
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
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        int topN = (s.getTopN() != null) ? s.getTopN() : defaults.getDefaultTopN();

        switch (data) {
            case "topn_inc"       -> topN++;
            case "topn_dec"       -> topN = Math.max(1, topN - 1);
            case "topn_default"   -> {
                settingsService.resetTopNDefaults(chatId);
                return name();
            }
            case "topn_save"      -> {
                settingsService.updateTopN(chatId, topN);
                return "ai_trading_settings";
            }
            case "topn_back"      -> {
                return "ai_trading_settings";
            }
            default               -> {
                return name();
            }
        }
        // при инкременте/декременте сразу сохраняем и перерисовываем
        settingsService.updateTopN(chatId, topN);
        return name();
    }
}
