package com.chicu.trader.bot.menu.feature.ai_trading.strategy.fibonacci;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_fibonacci_set_levels")
@RequiredArgsConstructor
public class AiTradingFibonacciSetLevelsState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_levels";
    }

    @Override
    public SendMessage render(Long chatId) {
        FibonacciGridStrategySettings s = settingsService.getOrCreate(chatId);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("−1").callbackData(name() + ":dec").build(),
                        InlineKeyboardButton.builder().text("Уровней: " + s.getGridLevels()).callbackData("noop").build(),
                        InlineKeyboardButton.builder().text("+1").callbackData(name() + ":inc").build()
                ),
                List.of(MenuUtils.backButton("ai_trading_fibonacci_config")) // ⬅ назад к меню конфигурации
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*Количество уровней сетки:* " + s.getGridLevels())
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        // Обработка кнопки "‹ Назад"
        if (data.equals("ai_trading_fibonacci_config")) {
            return "ai_trading_fibonacci_config"; // назад к общим настройкам
        }

        // Обработка кнопок увеличения/уменьшения
        FibonacciGridStrategySettings s = settingsService.getOrCreate(chatId);

        if (data.endsWith(":inc")) {
            s.setGridLevels(s.getGridLevels() + 1);
            settingsService.save(s);
        } else if (data.endsWith(":dec")) {
            int current = s.getGridLevels();
            if (current > 1) {
                s.setGridLevels(current - 1);
                settingsService.save(s);
            }
        }

        return name();
    }
}
