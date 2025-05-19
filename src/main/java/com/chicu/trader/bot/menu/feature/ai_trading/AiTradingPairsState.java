package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuSessionService;
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
    private final InlineKeyboardMarkup keyboard;

    public AiTradingPairsState(AiTradingSettingsService settingsService) {
        this.settingsService = settingsService;

        InlineKeyboardButton manual = InlineKeyboardButton.builder()
                .text("✏️ Ручной ввод").callbackData("pairs_manual").build();
        InlineKeyboardButton list   = InlineKeyboardButton.builder()
                .text("🔍 Выбор из списка").callbackData("pairs_list").build();
        InlineKeyboardButton ai     = InlineKeyboardButton.builder()
                .text("🤖 AI-подбор").callbackData("pairs_ai").build();
        InlineKeyboardButton def    = InlineKeyboardButton.builder()
                .text("🔄 Сброс").callbackData("pairs_default").build();
        InlineKeyboardButton back   = InlineKeyboardButton.builder()
                .text("‹ Назад").callbackData("pairs_back").build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                    List.of(manual),
                    List.of(list),
                    List.of(ai),
                    List.of(def),
                    List.of(back)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings_pairs";
    }

    @Override
    public SendMessage render(Long chatId) {
        String current = settingsService.getOrCreate(chatId).getSymbols();
        if (current == null || current.isBlank()) {
            current = "(все пары)";
        }
        String text = String.format("*Пары*\nТекущий: `%s`", current);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();
        return switch (data) {
            case "pairs_manual"  -> "ai_trading_pairs_manual";
            case "pairs_list"    -> "ai_trading_pairs_list";
            case "pairs_ai"      -> "ai_trading_pairs_ai";
            case "pairs_default" -> {
                settingsService.resetSymbolsDefaults(chatId);
                yield name();
            }
            case "pairs_back"    -> "ai_trading_settings";
            default              -> name();
        };
    }
}
