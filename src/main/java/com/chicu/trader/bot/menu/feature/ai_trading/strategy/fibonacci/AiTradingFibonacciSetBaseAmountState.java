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

@Component("ai_trading_fibonacci_set_base")
@RequiredArgsConstructor
public class AiTradingFibonacciSetBaseAmountState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_base";
    }

    @Override
    public SendMessage render(Long chatId) {
        var s = settingsService.getOrCreate(chatId);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("−10").callbackData(name() + ":dec").build(),
                        InlineKeyboardButton.builder().text("Базовая: " + s.getBaseAmount() + " USDT").callbackData("noop").build(),
                        InlineKeyboardButton.builder().text("+10").callbackData(name() + ":inc").build()
                ),
                List.of(MenuUtils.backButton("ai_trading_fibonacci_config"))
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*Базовая сумма входа:* " + s.getBaseAmount() + " USDT")
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        // Обработка кнопки Назад
        if (data.equals("ai_trading_fibonacci_config")) {
            return "ai_trading_fibonacci_config";
        }

        var s = settingsService.getOrCreate(chatId);

        if (data.endsWith(":inc")) {
            s.setBaseAmount(s.getBaseAmount() + 10.0);
            settingsService.save(s);
        } else if (data.endsWith(":dec")) {
            double newAmount = s.getBaseAmount() - 10.0;
            if (newAmount >= 10.0) {
                s.setBaseAmount(newAmount);
                settingsService.save(s);
            }
        }

        return name();
    }
}
