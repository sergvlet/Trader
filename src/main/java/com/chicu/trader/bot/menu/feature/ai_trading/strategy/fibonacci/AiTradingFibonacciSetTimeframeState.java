package com.chicu.trader.bot.menu.feature.ai_trading.strategy.fibonacci;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component("ai_trading_fibonacci_set_timeframe")
@RequiredArgsConstructor
@Slf4j
public class AiTradingFibonacciSetTimeframeState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    private static final List<String> OPTIONS = List.of("1m", "5m", "15m", "1h", "4h", "1d");

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_timeframe";
    }

    @Override
    public SendMessage render(Long chatId) {
        var s = settingsService.getOrCreate(chatId);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String tf : OPTIONS) {
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(tf.equals(s.getTimeframe()) ? "✅ " + tf : tf)
                    .callbackData(name() + ":" + tf)
                    .build()));
        }

        rows.add(List.of(MenuUtils.backButton("ai_trading_fibonacci_config")));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("*Выберите таймфрейм:*")
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        log.info("📥 Timeframe selection input: {}", data);

        // Если нажата кнопка "назад" — возвращаемся в конфигурацию стратегии
        if (data.equals("ai_trading_fibonacci_config")) {
            return "ai_trading_fibonacci_config";
        }

        if (!data.contains(":")) {
            log.warn("⚠️ Неверный формат callbackData: {}", data);
            return name();
        }

        String[] parts = data.split(":", 2);
        if (parts.length < 2) {
            log.warn("⚠️ Ошибка разбора callbackData: {}", data);
            return name();
        }

        String tf = parts[1];
        if (!OPTIONS.contains(tf)) {
            log.warn("⚠️ Неверный таймфрейм: {}", tf);
            return name();
        }

        var s = settingsService.getOrCreate(chatId);
        s.setTimeframe(tf);
        settingsService.save(s);

        log.info("✅ Установлен таймфрейм {} для chatId {}", tf, chatId);
        return name(); // остаёмся на текущем экране
    }

}
