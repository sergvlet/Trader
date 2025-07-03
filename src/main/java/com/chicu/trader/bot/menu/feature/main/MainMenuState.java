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
                                        .callbackData("ai_trading_settings")
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("✋ Ручная торговля")
                                        .callbackData("manual_trading_settings")
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
        return "main"; // Должен совпадать с fallback в MenuService
    }

    @Override
    public SendMessage render(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*🏠 Главное меню*\nВыберите один из разделов ниже:")
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            return switch (data) {
                case "ai_trading_settings" -> "ai_trading_settings";
                case "manual_trading_settings" -> "manual_trading_settings";
                case "about" -> "about";
                case "register" -> "register";
                case "plans" -> "plans";
                default -> name(); // fallback на главное меню
            };
        }
        return name();
    }
}
