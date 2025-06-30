package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.BacktestResult;
import com.chicu.trader.trading.service.BacktestService;
import com.chicu.trader.trading.service.ProfitablePairService;
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
    private final ProfitablePairService pairService;

    private final Map<Long, String> lastResults = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "ai_trading_settings_backtesting";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        List<ProfitablePair> pairs = pairService.getActivePairs(chatId);

        String strategy = Optional.ofNullable(settings.getStrategy()).map(StrategyType::name).orElse("N/A");
        double commission = Optional.ofNullable(settings.getCommission()).orElse(0.1);

        StringBuilder sb = new StringBuilder("*⚙️ Параметры стратегии:*\n");
        sb.append("• Стратегия: `").append(strategy).append("`\n");
        sb.append("• Комиссия: `").append(String.format("%.2f%%", commission)).append("`\n");

        if (pairs.isEmpty()) {
            sb.append("• Активные пары: _не выбраны_\n");
        } else {
            sb.append("• Пары и TP/SL:\n");
            for (ProfitablePair pair : pairs) {
                sb.append(String.format("  • `%s` → TP: %.2f%%, SL: %.2f%%\n",
                        pair.getSymbol(),
                        Optional.ofNullable(pair.getTakeProfitPct()).orElse(2.0),
                        Optional.ofNullable(pair.getStopLossPct()).orElse(1.0)));
            }
        }

        if (lastResults.containsKey(chatId)) {
            sb.append("\n").append(lastResults.remove(chatId)); // Показываем один раз
        } else {
            sb.append("\nНажмите «Запустить бэктест», чтобы протестировать стратегию по историческим данным.");
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(List.of(InlineKeyboardButton.builder()
                .text("▶️ Запустить бэктест")
                .callbackData(name() + ":run")
                .build()));
        keyboard.add(List.of(InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_settings")
                .build()));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(sb.toString())
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();
    }

    @Override
    public String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();

        String data = u.getCallbackQuery().getData();
        Long chatId = u.getCallbackQuery().getMessage().getChatId();

        if (data.endsWith(":run")) {
            BacktestResult result = backtestService.runBacktest(chatId);

            double pnl = result.getTotalPnl() * 100;
            double winRate = result.getWinRate() * 100;
            int count = result.getTotalTrades();

            StringBuilder msg = new StringBuilder();
            msg.append("*📈 Результаты бэктеста:*\n");
            msg.append(String.format("• Сделок: `%d`\n", count));
            msg.append(String.format("• Win-rate: `%.2f%%`\n", winRate));
            msg.append(String.format("• Общий PnL: `%.2f%%`\n", pnl));

            List<String> losers = result.getLosingSymbols();
            if (!losers.isEmpty()) {
                msg.append("\n⚠️ Убыточные пары:\n");
                for (String sym : losers) {
                    msg.append("• `").append(sym).append("`\n");
                }
            }

            msg.append("\n💡 Проверьте эффективность стратегии и скорректируйте TP/SL при необходимости.");
            lastResults.put(chatId, msg.toString());

            return name(); // возвращаем то же состояние
        }

        return "ai_trading_settings";
    }
}
