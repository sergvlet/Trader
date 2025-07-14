package com.chicu.trader.bot.menu.feature.ai_trading.strategy.fibonacci;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component("ai_trading_fibonacci_set_symbol")
@RequiredArgsConstructor
public class AiTradingFibonacciSetSymbolState implements MenuState {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_fibonacci_set_symbol";
    }

    @Override
    public SendMessage render(Long chatId) {
        FibonacciGridStrategySettings s = settingsService.getOrCreate(chatId);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите символ, например: *BTCUSDT*\n\nТекущий: `" + s.getSymbol() + "`")
                .parseMode("Markdown")
                .replyMarkup(MenuUtils.backKeyboard("ai_trading_fibonacci_config"))
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (update.hasCallbackQuery()) {
            // Возврат по кнопке Назад
            String data = update.getCallbackQuery().getData();
            if (data.equals("ai_trading_fibonacci_config")) {
                return data;
            }
        }

        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String text = message.getText();

            if (text == null || text.isBlank() || text.startsWith("/")) {
                MenuUtils.deferNotice(chatId, "⚠️ Символ не распознан. Попробуйте снова.");
                return name();
            }

            FibonacciGridStrategySettings s = settingsService.getOrCreate(chatId);
            s.setSymbol(text.toUpperCase().trim());
            settingsService.save(s);

            return "ai_trading_fibonacci_config";
        }

        return name();
    }
}
