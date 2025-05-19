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
public class AiTradingPairsAiState implements MenuState {

    private final AiTradingSettingsService settingsService;

    public AiTradingPairsAiState(AiTradingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public String name() {
        return "ai_trading_pairs_ai";
    }

    @Override
    public SendMessage render(Long chatId) {
        List<String> aiList = settingsService.suggestPairs(chatId);
        String text = "🤖 *AI-подбор пар*\n"
            + "Предложенные:\n"
            + String.join(",", aiList);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(List.of(
                    List.of(InlineKeyboardButton.builder()
                            .text("✅ Принять").callbackData("pairs_ai_confirm").build()),
                    List.of(InlineKeyboardButton.builder()
                            .text("🔄 Повторить").callbackData("pairs_ai_retry").build()),
                    List.of(InlineKeyboardButton.builder()
                            .text("‹ Назад").callbackData("pairs_ai_back").build())
                )).build())
                .build();
    }

    @Override
    public String handleInput(Update update) {
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (data) {
            case "pairs_ai_confirm" -> {
                List<String> aiList = settingsService.suggestPairs(chatId);
                settingsService.updateSymbols(chatId,
                        String.join(",", aiList));
                return "ai_trading_settings";
            }
            case "pairs_ai_retry"   -> {
                return name();
            }
            case "pairs_ai_back"    -> {
                return "ai_trading_settings";
            }
            default                  -> {
                return name();
            }
        }
    }
}
