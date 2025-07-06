package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import com.chicu.trader.trading.model.BacktestSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_backtesting_set_slippage")
@RequiredArgsConstructor
public class AiTradingBacktestingSetSlippageState implements MenuState {

    private static final double STEP = 0.1; // шаг в процентах

    private final BacktestSettingsService btService;

    @Override
    public String name() {
        return "ai_trading_backtesting_set_slippage";
    }

    @Override
    public SendMessage render(Long chatId) {
        BacktestSettings cfg = btService.getOrCreate(chatId);
        double slippage = cfg.getSlippagePct();

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
            List.of(
                InlineKeyboardButton.builder()
                    .text("➖")
                    .callbackData(name() + ":dec")
                    .build(),
                InlineKeyboardButton.builder()
                    .text(String.format("%.2f%%", slippage))
                    .callbackData(name() + ":noop")
                    .build(),
                InlineKeyboardButton.builder()
                    .text("➕")
                    .callbackData(name() + ":inc")
                    .build()
            ),
            List.of(
                InlineKeyboardButton.builder()
                    .text("‹ Назад")
                    .callbackData("ai_trading_backtesting_config")
                    .build()
            )
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(String.format("Установите проскальзывание (slippage):\nТекущее: %.2f%%  Шаг: %.2f%%",
                        slippage, STEP))
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update u) {
        String data  = u.getCallbackQuery().getData();
        long   chatId = u.getCallbackQuery().getMessage().getChatId();

        if ("ai_trading_backtesting_config".equals(data)) {
            return "ai_trading_backtesting_config";
        }

        BacktestSettings cfg = btService.getOrCreate(chatId);
        double cur = cfg.getSlippagePct();
        double updated = cur;

        switch (data) {
            case "ai_trading_backtesting_set_slippage:inc" -> updated = cur + STEP;
            case "ai_trading_backtesting_set_slippage:dec" -> updated = Math.max(0, cur - STEP);
            case "ai_trading_backtesting_set_slippage:noop"-> { return name(); }
        }

        btService.updateSlippage(chatId, updated);
        // просто перерисуем это же состояние с новым значением
        return name();
    }
}
