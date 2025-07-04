// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingBacktestingState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.backtest.BacktestResult;
import com.chicu.trader.trading.backtest.BacktestService;
import com.chicu.trader.trading.entity.ProfitablePair;
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
    private final ProfitablePairService pairService;
    private final BacktestService backtestService;

    // хранит последний результат, чтоб показать после рендера
    private final Map<Long, String> lastResults = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "ai_trading_settings_backtesting";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        List<ProfitablePair> pairs = pairService.getActivePairs(chatId);

        String strat = Optional.ofNullable(s.getStrategy()).map(StrategyType::name).orElse("N/A");
        double comm = Optional.ofNullable(s.getCommission()).orElse(0.0);

        StringBuilder md = new StringBuilder("*⚙️ Параметры стратегии:*\n");
        md.append("• Стратегия: `").append(strat).append("`\n");
        md.append("• Комиссия: `").append(String.format("%.2f%%", comm)).append("`\n");

        if (pairs.isEmpty()) {
            md.append("• Активные пары: _не выбраны_\n");
        } else {
            md.append("• Пары и TP/SL:\n");
            for (ProfitablePair p : pairs) {
                double tp = Optional.ofNullable(p.getTakeProfitPct()).orElse(0.0);
                double sl = Optional.ofNullable(p.getStopLossPct()).orElse(0.0);
                md.append(String.format("  • `%s` → TP: %.2f%%, SL: %.2f%%\n",
                        p.getSymbol(), tp, sl));
            }
        }

        if (lastResults.containsKey(chatId)) {
            md.append("\n").append(lastResults.remove(chatId));
        } else {
            md.append("\nНажмите ▶️ «Запустить бэктест», чтобы протестировать стратегию на истории.");
        }

        var kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(InlineKeyboardButton.builder()
                        .text("▶️ Запустить бэктест")
                        .callbackData(name() + ":run")
                        .build()),
                List.of(InlineKeyboardButton.builder()
                        .text("‹ Назад")
                        .callbackData("ai_trading_settings")
                        .build())
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(md.toString())
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();

        String data = u.getCallbackQuery().getData();
        Long chatId = u.getCallbackQuery().getMessage().getChatId();

        if (data.equals(name() + ":run")) {
            BacktestResult res = backtestService.runBacktest(chatId);
            double pnl     = res.getTotalPnl() * 100;
            double winRate = res.getWinRate() * 100;
            int trades     = res.getTotalTrades();

            StringBuilder out = new StringBuilder()
                    .append("*📈 Результаты бэктеста:*\n")
                    .append(String.format("• Сделок: `%d`\n", trades))
                    .append(String.format("• Win-rate: `%.2f%%`\n", winRate))
                    .append(String.format("• Общий PnL: `%.2f%%`\n", pnl));

            var losers = res.getLosingSymbols();
            if (!losers.isEmpty()) {
                out.append("\n⚠️ Убыточные пары:\n");
                losers.forEach(sym -> out.append("• `").append(sym).append("`\n"));
            }
            out.append("\n💡 Проверьте параметры и скорректируйте TP/SL при необходимости.");

            lastResults.put(chatId, out.toString());
        }

        return name();
    }
}
