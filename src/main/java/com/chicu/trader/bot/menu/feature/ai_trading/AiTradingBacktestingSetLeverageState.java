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

@Component("ai_trading_backtesting_set_leverage")
@RequiredArgsConstructor
public class AiTradingBacktestingSetLeverageState implements MenuState {

    private final BacktestSettingsService btService;

    @Override
    public String name() {
        return "ai_trading_backtesting_set_leverage";
    }

    @Override
    public SendMessage render(Long chatId) {
        int lev = btService.getOrCreate(chatId).getLeverage();

        String text = "*Плечо:*\nТекущее значение: `" + lev + "x`";

        var kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("−").callbackData(name() + ":dec").build(),
                        InlineKeyboardButton.builder().text("+").callbackData(name() + ":inc").build()
                ),
                List.of(InlineKeyboardButton.builder().text("‹ Назад").callbackData("ai_trading_backtesting_config").build())
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        BacktestSettings s = btService.getOrCreate(chatId);
        int lev = s.getLeverage();

        if (data.endsWith(":inc")) lev += 1;
        else if (data.endsWith(":dec")) lev = Math.max(1, lev - 1);

        btService.updateLeverage(chatId, lev);
        return name();
    }
}
