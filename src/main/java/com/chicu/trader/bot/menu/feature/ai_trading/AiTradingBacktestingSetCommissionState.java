package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import com.chicu.trader.trading.model.BacktestSettings;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_backtesting_set_commission")
@RequiredArgsConstructor
public class AiTradingBacktestingSetCommissionState implements MenuState {

    private final BacktestSettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_backtesting_set_commission";
    }

    @Override
    public SendMessage render(Long chatId) {
        BacktestSettings cfg = settingsService.getOrCreate(chatId);
        double commission = cfg.getCommissionPct();

        String text = "*Комиссия за сделку:*\n" +
                String.format("Текущая: `%.2f%%`\n", commission) +
                "\nИспользуйте кнопки ниже для изменения.";

        var keyboard = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        InlineKeyboardButton.builder().text("− 0.1%").callbackData(name() + ":dec").build(),
                        InlineKeyboardButton.builder().text("+ 0.1%").callbackData(name() + ":inc").build()
                ),
                List.of(
                        InlineKeyboardButton.builder().text("‹ Назад").callbackData("ai_trading_backtesting_config").build()
                )
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        BacktestSettings cfg = settingsService.getOrCreate(chatId);
        double current = cfg.getCommissionPct();

        if (data.endsWith(":dec")) {
            double newValue = Math.max(0.0, current - 0.1);
            settingsService.updateCommission(chatId, newValue);
            return name(); // остаться в этом меню
        } else if (data.endsWith(":inc")) {
            double newValue = current + 0.1;
            settingsService.updateCommission(chatId, newValue);
            return name(); // остаться
        } else if (data.equals("ai_trading_backtesting_config")) {
            return "ai_trading_backtesting_config"; // вернуться назад
        }

        return name();
    }
}
