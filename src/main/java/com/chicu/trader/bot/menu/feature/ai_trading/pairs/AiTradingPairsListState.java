// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingPairsListState.java
package com.chicu.trader.bot.menu.feature.ai_trading.pairs;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiTradingPairsListState implements MenuState {

    private final AiTradingSettingsService settingsService;

    public AiTradingPairsListState(AiTradingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public String name() {
        return "ai_trading_pairs_list";
    }

    @Override
    public SendMessage render(Long chatId) {
        List<String> pairs = settingsService.suggestPairs(chatId);

        var rows = pairs.stream()
            .map(sym -> List.of(
                InlineKeyboardButton.builder()
                    .text(sym)
                    .callbackData("pair_select:" + sym)
                    .build()
            ))
            .collect(Collectors.toList());

        // кнопка «Назад» внизу
        rows.add(List.of(
            InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_settings")
                .build()
        ));

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
            .keyboard(rows)
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
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("pair_select:")) {
            String sym = data.substring("pair_select:".length());
            settingsService.updateSymbols(chatId, sym);
            return "ai_trading_settings";
        }
        // «Назад»
        if ("ai_trading_settings".equals(data)) {
            return "ai_trading_settings";
        }
        return name();
    }
}
