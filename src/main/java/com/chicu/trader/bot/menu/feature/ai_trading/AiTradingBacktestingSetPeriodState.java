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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component("ai_trading_backtesting_set_period")
@RequiredArgsConstructor
public class AiTradingBacktestingSetPeriodState implements MenuState {

    private final BacktestSettingsService backtestSettingsService;

    /** Какой месяц/год сейчас показываем в календаре для каждого чата */
    private final Map<Long, YearMonth> viewMonth = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public String name() {
        return "ai_trading_backtesting_set_period";
    }

    @Override
    public SendMessage render(Long chatId) {
        BacktestSettings cfg = backtestSettingsService.getOrCreate(chatId);

        // Инициализируем видимый месяц текущей viewMonth или текущим месяцем
        YearMonth currentMonth = YearMonth.now();
        YearMonth ym = viewMonth.computeIfAbsent(chatId, id -> currentMonth);

        // Текущее отображение периода: от startDate до "сегодня"
        String text = "*📅 Установить дату начала бэктеста*\n"
            + "Сейчас: `"
            + cfg.getStartDate().format(DF)
            + "` → `"
            + LocalDate.now().format(DF)
            + "`\n\n"
            + "_Выберите день для начала:_";

        InlineKeyboardMarkup kb = buildCalendar(ym);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update u) {
        String data   = u.getCallbackQuery().getData();
        long   chatId = u.getCallbackQuery().getMessage().getChatId();

        // Навигация по годам/месяцам
        if (data.startsWith("nav_prev_year_")) {
            YearMonth ym = YearMonth.parse(data.substring("nav_prev_year_".length()));
            viewMonth.put(chatId, ym.minusYears(1));
            return name();
        }
        if (data.startsWith("nav_next_year_")) {
            YearMonth ym = YearMonth.parse(data.substring("nav_next_year_".length()));
            viewMonth.put(chatId, ym.plusYears(1));
            return name();
        }
        if (data.startsWith("nav_prev_month_")) {
            YearMonth ym = YearMonth.parse(data.substring("nav_prev_month_".length()));
            viewMonth.put(chatId, ym.minusMonths(1));
            return name();
        }
        if (data.startsWith("nav_next_month_")) {
            YearMonth ym = YearMonth.parse(data.substring("nav_next_month_".length()));
            viewMonth.put(chatId, ym.plusMonths(1));
            return name();
        }

        // Отмена и возврат в главное меню бэктеста
        if ("back".equals(data)) {
            viewMonth.remove(chatId);
            return "ai_trading_backtesting_config";
        }

        // Выбран конкретный день
        String prefix = name() + ":";
        if (data.startsWith(prefix)) {
            LocalDate newStart = LocalDate.parse(data.substring(prefix.length()));
            LocalDate today    = LocalDate.now();

            // Сохраняем новый период: от newStart до сегодня
            BacktestSettings cfg = backtestSettingsService.getOrCreate(chatId);
            cfg.setStartDate(newStart);
            cfg.setEndDate(today);
            backtestSettingsService.save(cfg);

            viewMonth.remove(chatId);
            return "ai_trading_backtesting_config";
        }

        return name();
    }

    private InlineKeyboardMarkup buildCalendar(YearMonth ym) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Навигационные кнопки
        rows.add(List.of(
            btn("««", "nav_prev_year_"  + ym),
            btn("‹",  "nav_prev_month_" + ym),
            InlineKeyboardButton.builder()
                .text(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                      + " " + ym.getYear())
                .callbackData("noop")
                .build(),
            btn("›",  "nav_next_month_" + ym),
            btn("››", "nav_next_year_"  + ym)
        ));

        // Заголовок дней недели
        List<InlineKeyboardButton> header = new ArrayList<>();
        for (DayOfWeek dow : DayOfWeek.values()) {
            header.add(InlineKeyboardButton.builder()
                .text(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                .callbackData("noop")
                .build());
        }
        rows.add(header);

        // Заполняем числа месяца
        LocalDate first = ym.atDay(1);
        int emptyCells = first.getDayOfWeek().getValue() % 7;
        int daysInMonth = ym.lengthOfMonth();
        List<InlineKeyboardButton> week = new ArrayList<>();

        // Пустые ячейки перед первым
        for (int i = 0; i < emptyCells; i++) {
            week.add(InlineKeyboardButton.builder()
                .text(" ")
                .callbackData("noop")
                .build());
        }

        // Кнопки-даты
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);
            // Разрешаем выбирать только прошлое и сегодня
            if (!date.isAfter(LocalDate.now())) {
                week.add(btn(String.valueOf(day), name() + ":" + date));
            } else {
                week.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("noop")
                    .build());
            }
            if (week.size() == 7) {
                rows.add(week);
                week = new ArrayList<>();
            }
        }
        // Дозаполняем последний ряд
        if (!week.isEmpty()) {
            while (week.size() < 7) {
                week.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("noop")
                    .build());
            }
            rows.add(week);
        }

        // Кнопка «Назад»
        rows.add(List.of(
            InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("back")
                .build()
        ));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btn(String text, Object data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data.toString())
                .build();
    }
}
