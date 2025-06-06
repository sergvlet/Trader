package com.chicu.trader.bot.menu.feature.ai_trading.pairs;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.TradingExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class AiTradingPairsManualState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final TradingExecutor tradingExecutor;

    public AiTradingPairsManualState(AiTradingSettingsService settingsService, TradingExecutor tradingExecutor) {
        this.settingsService = settingsService;
        this.tradingExecutor = tradingExecutor;
    }

    @Override
    public String name() {
        return "ai_trading_pairs_manual";
    }

    @Override
    public SendMessage render(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("✏️ *Ручной ввод пар*\n"
                        + "Отправьте список через запятую, например:\n"
                        + "`BTCUSDT,ETHUSDT,BNBUSDT`")
                .parseMode("Markdown")
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText().trim().toUpperCase();
            List<String> symbols = List.of(text.split(","));
            settingsService.updateSymbols(chatId, text);
            tradingExecutor.updateExecutor(chatId, symbols); // ← обновление
            return "ai_trading_settings";
        }
        return name();
    }
}
