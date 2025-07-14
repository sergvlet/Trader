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

@Component("ai_trading_fibonacci_set_tp_sl")
@RequiredArgsConstructor
public class AiTradingFibonacciSetTpSlState implements MenuState {
    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_tp_sl";
    }

    @Override
    public SendMessage render(Long chatId) {
        var s = settingsService.getOrCreate(chatId);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("TP −0.1%").callbackData(name() + ":tp_dec").build(),
                        InlineKeyboardButton.builder().text("TP: " + s.getTakeProfitPct() + "%").callbackData("noop").build(),
                        InlineKeyboardButton.builder().text("TP +0.1%").callbackData(name() + ":tp_inc").build()
                ),
                List.of(
                        InlineKeyboardButton.builder().text("SL −0.1%").callbackData(name() + ":sl_dec").build(),
                        InlineKeyboardButton.builder().text("SL: " + s.getStopLossPct() + "%").callbackData("noop").build(),
                        InlineKeyboardButton.builder().text("SL +0.1%").callbackData(name() + ":sl_inc").build()
                ),
                List.of(MenuUtils.backButton("ai_trading_fibonacci_config"))
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*Take Profit:* " + s.getTakeProfitPct() + "%\n*Stop Loss:* " + s.getStopLossPct() + "%")
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        var s = settingsService.getOrCreate(chatId);
        // Обработка кнопки Назад
        if (data.equals("ai_trading_fibonacci_config")) {
            return "ai_trading_fibonacci_config";
        }
        if (data.endsWith(":tp_inc")) s.setTakeProfitPct(s.getTakeProfitPct() + 0.1);
        if (data.endsWith(":tp_dec")) s.setTakeProfitPct(Math.max(0.1, s.getTakeProfitPct() - 0.1));

        if (data.endsWith(":sl_inc")) s.setStopLossPct(s.getStopLossPct() + 0.1);
        if (data.endsWith(":sl_dec")) s.setStopLossPct(Math.max(0.1, s.getStopLossPct() - 0.1));

        settingsService.save(s);
        return name();
    }
}
