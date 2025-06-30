package com.chicu.trader.bot.menu.feature.ai_trading.pairs;

import com.chicu.trader.bot.menu.core.MenuState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_pairs")
public class AiTradingPairsState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public AiTradingPairsState() {
        InlineKeyboardButton fromList = InlineKeyboardButton.builder()
                .text("📋 Выбрать из списка")
                .callbackData("pairs_from_list")
                .build();

        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("pairs_back")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(fromList),
                        List.of(back)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_pairs";
    }

    @Override
    public SendMessage render(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите способ изменения торговых пар:")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();

        String data = update.getCallbackQuery().getData();
        return switch (data) {
            case "pairs_from_list" -> "ai_trading_pairs_list"; // переход к списку
            case "pairs_back"      -> "ai_trading_settings";   // назад в настройки
            default                -> name();
        };
    }
}
