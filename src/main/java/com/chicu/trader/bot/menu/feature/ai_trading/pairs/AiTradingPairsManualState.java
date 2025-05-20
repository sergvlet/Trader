package com.chicu.trader.bot.menu.feature.ai_trading.pairs;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class AiTradingPairsManualState implements MenuState {

    private final AiTradingSettingsService settingsService;

    public AiTradingPairsManualState(AiTradingSettingsService settingsService) {
        this.settingsService = settingsService;
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
            Long chatId    = update.getMessage().getChatId();
            String text    = update.getMessage().getText().trim().toUpperCase();
            // можно добавить валидацию по паттерну
            settingsService.updateSymbols(chatId, text);
            return "ai_trading_settings";
        }
        return name();
    }
}
