package com.chicu.trader.bot.menu.feature.ai_trading.strategy.fibonacci;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_fibonacci_set_candles")
@RequiredArgsConstructor
public class AiTradingFibonacciSetCandlesState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_candles";
    }

    @Override
    public SendMessage render(Long chatId) {
        var s = settingsService.getOrCreate(chatId);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("−10").callbackData(name() + ":dec").build(),
                        InlineKeyboardButton.builder().text("Свечей: " + s.getCachedCandlesLimit()).callbackData("noop").build(),
                        InlineKeyboardButton.builder().text("+10").callbackData(name() + ":inc").build()
                ),
                List.of(MenuUtils.backButton("ai_trading_fibonacci_config"))
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*Количество свечей для анализа:* " + s.getCachedCandlesLimit())
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        var s = settingsService.getOrCreate(chatId);
        String data = update.getCallbackQuery().getData();
        // Обработка кнопки Назад
        if (data.equals("ai_trading_fibonacci_config")) {
            return "ai_trading_fibonacci_config";
        }
        if (data.endsWith(":inc")) {
            s.setCachedCandlesLimit(s.getCachedCandlesLimit() + 10);
        } else if (data.endsWith(":dec")) {
            int c = s.getCachedCandlesLimit();
            if (c > 10) s.setCachedCandlesLimit(c - 10);
        }

        settingsService.save(s);
        return name();
    }
}
