package com.chicu.trader.bot.menu.feature.main;

import com.chicu.trader.bot.menu.core.MenuState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class MainMenuState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public MainMenuState() {
        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(
                    InlineKeyboardButton.builder()
                        .text("🤖 AI-торговля")
                        .callbackData("ai_trading")
                        .build(),
                    InlineKeyboardButton.builder()
                        .text("✋ Ручная торговля")
                        .callbackData("manual_trading")
                        .build()
                ),
                List.of(
                    InlineKeyboardButton.builder()
                        .text("ℹ️ О боте")
                        .callbackData("about")
                        .build(),
                    InlineKeyboardButton.builder()
                        .text("📝 Регистрация")
                        .callbackData("register")
                        .build()
                ),
                List.of(
                    InlineKeyboardButton.builder()
                        .text("💳 Тарифы")
                        .callbackData("plans")
                        .build()
                )
            ))
            .build();
    }

    @Override
    public String name() {
        return "MAIN_MENU";
    }

    @Override
    public SendMessage render(Long chatId) {
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text("🔹 Главное меню — выберите раздел:")
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public String handleInput(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getData();  // "ai_trading", "about" и т.д.
        }
        return name();
    }
}
