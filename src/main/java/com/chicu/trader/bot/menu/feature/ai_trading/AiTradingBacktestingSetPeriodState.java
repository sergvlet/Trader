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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component("ai_trading_backtesting_set_period")
@RequiredArgsConstructor
public class AiTradingBacktestingSetPeriodState implements MenuState {

    private final BacktestSettingsService backtestSettingsService;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public String name() {
        return "ai_trading_backtesting_set_period";
    }

    @Override
    public SendMessage render(Long chatId) {
        BacktestSettings settings = backtestSettingsService.getOrCreate(chatId);

        String text = "*📅 Выберите период:*\n"
                + "Начало: `" + settings.getStartDate().format(DF) + "`\n"
                + "Конец: `" + settings.getEndDate().format(DF) + "`";

        var kb = buildCalendarKeyboard("start_", settings.getStartDate());

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text + "\n\nВыберите дату начала:")
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        String data = update.getCallbackQuery().getData(); // например "start_2025-07-01" или "end_2025-07-10"
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("start_")) {
            LocalDate date = LocalDate.parse(data.substring("start_".length()));
            BacktestSettings cfg = backtestSettingsService.getOrCreate(chatId);
            cfg.setStartDate(date);
            backtestSettingsService.save(cfg);
            return "ai_trading_backtesting_set_period_end";
        }

        if (data.startsWith("end_")) {
            LocalDate date = LocalDate.parse(data.substring("end_".length()));
            BacktestSettings cfg = backtestSettingsService.getOrCreate(chatId);
            cfg.setEndDate(date);
            backtestSettingsService.save(cfg);
            return "ai_trading_backtesting_config";
        }

        return name();
    }

    private InlineKeyboardMarkup buildCalendarKeyboard(String prefix, LocalDate base) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                LocalDate d = base.minusDays(14).plusDays(i * 4L + j);
                row.add(InlineKeyboardButton.builder()
                        .text(DF.format(d))
                        .callbackData(prefix + d.toString())
                        .build());
            }
            rows.add(row);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
