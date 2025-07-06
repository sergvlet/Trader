package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.stream.Collectors;

@Component("ai_trading_backtesting_set_timeframe")
@RequiredArgsConstructor
public class AiTradingBacktestingSetTimeframeState implements MenuState {

    private final BacktestSettingsService btService;

    @Override
    public String name() {
        return "ai_trading_backtesting_set_timeframe";
    }

    @Override
    public SendMessage render(Long chatId) {
        String current = btService.getOrCreate(chatId).getTimeframe();
        String text = "*Выберите таймфрейм:*\nТекущий: `" + current + "`";

        List<String> options = List.of("1m", "5m", "15m", "1h", "4h", "1d");

        List<List<InlineKeyboardButton>> rows = options.stream().map(tf ->
                List.of(InlineKeyboardButton.builder()
                        .text(tf)
                        .callbackData(name() + ":" + tf).build())
        ).collect(Collectors.toList());

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_backtesting_config").build()));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        String tf = data.substring(data.lastIndexOf(":") + 1);
        btService.updateTimeframe(chatId, tf);

        return "ai_trading_backtesting_config";
    }
}
