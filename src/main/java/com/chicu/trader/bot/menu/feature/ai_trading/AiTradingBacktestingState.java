package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.model.BacktestResult;
import com.chicu.trader.trading.service.BacktestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AiTradingBacktestingState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final BacktestService backtestService;

    // Храним результат на время отрисовки
    private final Map<Long, String> lastResults = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "ai_trading_settings_backtesting";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);

        // Параметры
        String strategy = Optional.ofNullable(s.getStrategy()).map(StrategyType::name).orElse("N/A");
        String pairs = Optional.ofNullable(s.getSymbols()).orElse("—");
        double tp = Optional.ofNullable(s.getRiskThreshold()).orElse(2.0);
        double sl = Optional.ofNullable(s.getMaxDrawdown()).orElse(1.0);
        double commission = Optional.ofNullable(s.getCommission()).orElse(0.1);

        StringBuilder sb = new StringBuilder("*⚙️ Параметры стратегии:*\n");
        sb.append("• Стратегия: `").append(strategy).append("`\n");
        sb.append("• Пары: `").append(pairs).append("`\n");
        sb.append(String.format("• Take-Profit: `%.2f%%`\n", tp));
        sb.append(String.format("• Stop-Loss: `%.2f%%`\n", sl));
        sb.append(String.format("• Комиссия: `%.2f%%`\n", commission));
        sb.append("\nНажмите «Запустить бэктест», чтобы протестировать стратегию по историческим данным.\n");

        if (lastResults.containsKey(chatId)) {
            sb.append("\n").append(lastResults.remove(chatId)); // показать 1 раз
        }

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(InlineKeyboardButton.builder()
                .text("▶️ Запустить бэктест")
                .callbackData("ai_trading_settings_backtesting:run")
                .build()));
        buttons.add(List.of(InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_settings")
                .build()));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(sb.toString())
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                .build();
    }

    @Override
    public String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();
        String data = u.getCallbackQuery().getData();
        Long chatId = u.getCallbackQuery().getMessage().getChatId();

        if ("ai_trading_settings_backtesting:run".equals(data)) {
            BacktestResult result = backtestService.runBacktest(chatId);

            double pnl = result.getTotalPnl();
            double winRate = result.getWinRate() * 100;
            int count = result.getTrades().size();

            String msg = String.format("""
                *📈 Результаты бэктеста:*
                • Сделок: `%d`
                • Win-rate: `%.2f%%`
                • Общий PnL: `%.2f%%`

                💡 Проверьте эффективность стратегии и измените TP/SL при необходимости.
                """, count, winRate, pnl * 100);

            lastResults.put(chatId, msg);
            return name(); // перерисовать то же состояние с результатом
        }

        if ("ai_trading_settings".equals(data)) {
            return "ai_trading_settings";
        }

        return name();
    }
}
