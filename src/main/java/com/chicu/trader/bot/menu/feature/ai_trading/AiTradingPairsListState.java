// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingPairsListState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiTradingPairsListState implements MenuState {

    private static final String STATE_AI_TRADING_SETTINGS = "ai_trading_settings";

    private final AiTradingSettingsService svc;

    @Override
    public String name() {
        return "ai_trading_settings_pairs";
    }

    @Override
    public SendMessage render(Long chatId) {
        List<String> pairs = svc.suggestPairs(chatId);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(
                        pairs.stream()
                                .map(sym -> List.of(
                                        InlineKeyboardButton.builder()
                                                .text(sym)
                                                .callbackData("pair_select:" + sym)
                                                .build()
                                ))
                                .toList()
                )
                .build();

        String text = pairs.isEmpty()
                ? "Нет доступных пар для выбора."
                : "Выберите одну из доступных пар:";

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (STATE_AI_TRADING_SETTINGS.equals(data)) {
            return STATE_AI_TRADING_SETTINGS;
        }
        if (data.startsWith("pair_select:")) {
            String sym = data.substring("pair_select:".length());
            svc.updateSymbols(chatId, sym);
            return STATE_AI_TRADING_SETTINGS;
        }
        return name();
    }
}
