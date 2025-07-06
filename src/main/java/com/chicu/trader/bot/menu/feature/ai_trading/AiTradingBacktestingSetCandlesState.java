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

@Component("ai_trading_backtesting_set_candles")
@RequiredArgsConstructor
public class AiTradingBacktestingSetCandlesState implements MenuState {

    private final BacktestSettingsService btService;

    @Override
    public String name() {
        return "ai_trading_backtesting_set_candles";
    }

    @Override
    public SendMessage render(Long chatId) {
        int count = btService.getOrCreate(chatId).getCachedCandlesLimit();

        String text = "*Количество свечей для кэша:*\n" +
                "Текущее значение: `" + count + "`";

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder()
                                .text("−")
                                .callbackData(name() + ":dec")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("+")
                                .callbackData(name() + ":inc")
                                .build()
                ),
                List.of(
                        InlineKeyboardButton.builder()
                                .text("‹ Назад")
                                // возвращаем callback в главное меню бэктеста
                                .callbackData("ai_trading_backtesting_config")
                                .build()
                )
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
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();

        // Если нажали «Назад» — возвращаемся в меню конфигурации бэктеста
        if ("ai_trading_backtesting_config".equals(data)) {
            return "ai_trading_backtesting_config";
        }

        // Иначе — изменяем число свечей и остаёмся в этом же состоянии
        BacktestSettings s = btService.getOrCreate(chatId);
        int val = s.getCachedCandlesLimit();

        if (data.endsWith(":inc")) {
            val += 50;
        } else if (data.endsWith(":dec")) {
            val = Math.max(50, val - 50);
        }

        btService.updateCachedCandlesLimit(chatId, val);
        return name();
    }
}
