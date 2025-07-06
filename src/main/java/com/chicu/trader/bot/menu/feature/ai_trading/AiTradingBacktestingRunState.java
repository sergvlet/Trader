package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.trading.backtest.BacktestResult;
import com.chicu.trader.trading.backtest.service.BacktestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Component("ai_trading_backtesting_execute")
@RequiredArgsConstructor
public class AiTradingBacktestingRunState implements MenuState {

    private final BacktestService backtestService;

    @Override
    public String name() {
        return "ai_trading_backtesting_execute";
    }

    @Override
    public SendMessage render(Long chatId) {
        log.info("▶️ Запуск бэктеста для chatId={}", chatId);

        BacktestResult result = backtestService.runBacktest(chatId);
        String message = buildSummary(result);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .parseMode("Markdown")
                .build();
    }

    @Override
    public String handleInput(Update update) {
        return "ai_trading_backtesting_config"; // Возврат в меню настроек бэктеста
    }

    private String buildSummary(BacktestResult result) {
        double pnl = result.getTotalPnl() * 100.0;
        double winRate = result.getWinRate() * 100.0;
        int count = result.getTotalTrades();

        StringBuilder sb = new StringBuilder("*📈 Результаты бэктеста:*\n");
        sb.append(String.format("• Сделок: `%d`\n", count));
        sb.append(String.format("• Win-rate: `%.2f%%`\n", winRate));
        sb.append(String.format("• Общий PnL: `%.2f%%`\n", pnl));

        List<String> losers = result.getLosingSymbols();
        if (!losers.isEmpty()) {
            sb.append("\n⚠️ Убыточные пары:\n");
            for (String symbol : losers) {
                sb.append("• `").append(symbol).append("`\n");
            }
        }

        sb.append("\n💡 Проанализируйте TP/SL и стратегию при необходимости.");
        return sb.toString();
    }
}
