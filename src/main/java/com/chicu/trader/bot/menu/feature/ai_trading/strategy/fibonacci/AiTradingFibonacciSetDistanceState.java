package com.chicu.trader.bot.menu.feature.ai_trading.strategy.fibonacci;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_fibonacci_set_distance")
@RequiredArgsConstructor
public class AiTradingFibonacciSetDistanceState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_distance";
    }

    @Override
    public SendMessage render(Long chatId) {
        var s = settingsService.getOrCreate(chatId);
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("−0.1%").callbackData(name() + ":dec").build(),
                        InlineKeyboardButton.builder().text("Шаг: " + s.getDistancePct() + "%").callbackData("noop").build(),
                        InlineKeyboardButton.builder().text("+0.1%").callbackData(name() + ":inc").build()
                ),
                List.of(MenuUtils.backButton("ai_trading_fibonacci_config"))
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*Расстояние между уровнями:* " + s.getDistancePct() + "%")
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        var s = settingsService.getOrCreate(chatId);

        if (data.endsWith(":inc")) {
            s.setDistancePct(s.getDistancePct() + 0.1);
        } else if (data.endsWith(":dec")) {
            double val = s.getDistancePct();
            if (val > 0.1) s.setDistancePct(Math.round((val - 0.1) * 10.0) / 10.0);
        }

        settingsService.save(s);
        return name();
    }
}
