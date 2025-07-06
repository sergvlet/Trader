package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Экран «📊 Статистика» для AI-торговли
 */
@Component
@RequiredArgsConstructor
public class AiTradingStatisticsState implements MenuState {

    private final AiTradingService aiService;

    @Override
    public String name() {
        return "ai_trading_statistics";
    }

    @Override
    public SendMessage render(Long chatId) {
        // Пример: здесь можно вытянуть реальные данные из aiService
        String text = "*Статистика AI-торговли*\n" +
            "Всего сделок: 123\n" +
            "Успешных: 98\n" +
            "PnL: +12.34%\n\n" +
            "Подробнее — в будущем.";
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData("ai_trading")
            .build();
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(List.of(backBtn)))
            .build();

        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(kb)
            .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        if (update.hasCallbackQuery()
         && "ai_trading".equals(update.getCallbackQuery().getData())) {
            return "ai_trading";
        }
        return name();
    }
}
