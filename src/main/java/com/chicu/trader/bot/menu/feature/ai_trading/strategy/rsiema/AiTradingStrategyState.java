// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/strategy/rsiema/AiTradingStrategyState.java
package com.chicu.trader.bot.menu.feature.ai_trading.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiTradingStrategyState implements MenuState {

    private final AiTradingSettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_settings_strategy";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        StrategyType current = s.getStrategy();

        String sb = "*Выбор стратегии AI*\n\n" +
                "Текущая: " + current.getLabel() + "\n\n" +
                "Выберите одну из доступных стратегий:";

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (StrategyType type : StrategyType.values()) {
            boolean selected = type == current;
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text((selected ? "✅ " : "") + type.getLabel())
                    .callbackData("strategy_select:" + type.name())
                    .build();
            rows.add(List.of(btn));
        }

        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_settings")
                .build();
        rows.add(List.of(back));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(sb)
                .parseMode("Markdown")
                .replyMarkup(new InlineKeyboardMarkup(rows))
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("strategy_select:")) {
            String code = data.substring("strategy_select:".length());
            settingsService.updateStrategy(chatId, code);

            // переходим в подменю, если есть для стратегии
            return switch (StrategyType.findByCode(code)) {
                case RSI_EMA -> "rsi_ema_config";
                // case SCALPING -> "scalping_config"; // по аналогии
                default -> name();
            };
        }

        if ("ai_trading_settings".equals(data)) {
            return "ai_trading_settings";
        }

        return name();
    }
}
