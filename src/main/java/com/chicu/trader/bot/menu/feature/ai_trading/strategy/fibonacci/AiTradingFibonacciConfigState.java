package com.chicu.trader.bot.menu.feature.ai_trading.strategy.fibonacci;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("ai_trading_fibonacci_config")
@RequiredArgsConstructor
public class AiTradingFibonacciConfigState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_config";
    }

    @Override
    public SendMessage render(Long chatId) {
        FibonacciGridStrategySettings s = settingsService.getOrCreate(chatId);

        String text = """
                *⚙️ Fibonacci Grid Strategy*

                • Пара: `%s`
                • Уровней сетки: `%d`
                • Расстояние между уровнями: `%.2f%%`
                • Базовая сумма: `%.2f USDT`
                • TP: `%.2f%%` | SL: `%.2f%%`
                • Таймфрейм: `%s`
                • Свечей в кэше: `%d`

                Выберите параметр для изменения:
                """.formatted(
                s.getSymbol(),
                s.getGridLevels(),
                s.getDistancePct(),
                s.getBaseAmount(),
                s.getTakeProfitPct(),
                s.getStopLossPct(),
                s.getTimeframe(),
                s.getCachedCandlesLimit()
        );

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(btn("📈 Пара", ":symbol"), btn("📊 Уровни", ":levels")),
                List.of(btn("📏 Расстояние", ":distance"), btn("💵 Сумма", ":base")),
                List.of(btn("🎯 TP/SL", ":tp_sl"), btn("⏱ Таймфрейм", ":timeframe")),
                List.of(btn("🧠 Кэш", ":candles")),
                List.of(MenuUtils.backButton("ai_trading_settings"))
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        String data = update.getCallbackQuery().getData();

        return switch (data) {
            case "ai_trading_fibonacci_config:symbol"    -> "ai_trading_fibonacci_set_symbol";
            case "ai_trading_fibonacci_config:levels"    -> "ai_trading_fibonacci_set_levels";
            case "ai_trading_fibonacci_config:distance"  -> "ai_trading_fibonacci_set_distance";
            case "ai_trading_fibonacci_config:base"      -> "ai_trading_fibonacci_set_base";
            case "ai_trading_fibonacci_config:tp_sl"     -> "ai_trading_fibonacci_set_tp_sl";
            case "ai_trading_fibonacci_config:timeframe" -> "ai_trading_fibonacci_set_timeframe";
            case "ai_trading_fibonacci_config:candles"   -> "ai_trading_fibonacci_set_candles";
            default -> "ai_trading_settings";
        };
    }

    private InlineKeyboardButton btn(String text, String suffix) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(name() + suffix)
                .build();
    }
}
