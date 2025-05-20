package com.chicu.trader.bot.menu.feature.ai_trading.order;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Экран «📋 Ордера» для AI-торговли
 */
@Component
@RequiredArgsConstructor
public class AiTradingOrdersState implements MenuState {

    private final AiTradingService aiService;

    @Override
    public String name() {
        return "ai_trading_orders";
    }

    @Override
    public SendMessage render(Long chatId) {
        // Здесь можно получить из aiService список последних ордеров
        String text = "*Последние ордера AI-торговли*\n" +
            "1) BUY BTC/USDT @ 30000 USDT\n" +
            "2) SELL ETH/USDT @ 2000 USDT\n" +
            "...\n\n" +
            "В будущем — полный лог.";
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
    public String handleInput(Update update) {
        if (update.hasCallbackQuery()
         && "ai_trading".equals(update.getCallbackQuery().getData())) {
            return "ai_trading";
        }
        return name();
    }
}
