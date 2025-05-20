// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingPairsState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AiTradingPairsState implements MenuState {

    private final AiTradingSettingsService settingsService;

    public AiTradingPairsState(AiTradingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public String name() {
        return "ai_trading_settings_pairs";
    }

    @Override
    public SendMessage render(Long chatId) {
        InlineKeyboardButton manual = InlineKeyboardButton.builder()
            .text("✏️ Ввести вручную")
            .callbackData("pairs_manual")
            .build();
        InlineKeyboardButton list = InlineKeyboardButton.builder()
            .text("📋 Из списка")
            .callbackData("pairs_list")
            .build();
        InlineKeyboardButton ai = InlineKeyboardButton.builder()
            .text("🤖 AI-подбор")
            .callbackData("pairs_ai")
            .build();
        InlineKeyboardButton def = InlineKeyboardButton.builder()
            .text("🔄 По умолчанию")
            .callbackData("pairs_default")
            .build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData("pairs_back")
            .build();

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(manual, list),
                List.of(ai, def),
                List.of(back)
            ))
            .build();

        String current = settingsService.getOrCreate(chatId).getSymbols();
        String text = "*Пары для торговли*\n"
            + "Текущие: `" + (current == null || current.isBlank() ? "—" : current) + "`\n\n"
            + "Выберите способ задания:";

        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(kb)
            .build();
    }

    @Override
    public String handleInput(Update update) {
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (data) {
            case "pairs_manual":
                return "ai_trading_pairs_manual";
            case "pairs_list":
                return "ai_trading_pairs_list";
            case "pairs_ai":
                return "ai_trading_pairs_ai";
            case "pairs_default":
                settingsService.resetSymbolsDefaults(chatId);
                return name();
            case "pairs_back":
                return "ai_trading_settings";
            default:
                return name();
        }
    }
}
