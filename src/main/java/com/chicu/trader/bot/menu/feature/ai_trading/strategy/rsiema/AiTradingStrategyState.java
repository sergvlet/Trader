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
        String current = s.getStrategy();

        StringBuilder sb = new StringBuilder();
        sb.append("*Выбор стратегии AI*\n\n");
        sb.append("Текущая: ").append(StrategyType.findByCode(current).getLabel()).append("\n\n");
        sb.append("Выберите одну из доступных стратегий:");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (StrategyType type : StrategyType.values()) {
            boolean selected = type.name().equals(current);
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
                .text(sb.toString())
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("strategy_select:")) {
            String code = data.substring("strategy_select:".length());
            settingsService.updateStrategy(chatId, code);

            // переход в подменю, если нужно
            return switch (code) {
                case "RSI_EMA" -> "rsi_ema_config";
                default -> name();
            };
        }

        if ("ai_trading_settings".equals(data)) {
            return "ai_trading_settings";
        }

        return name();
    }
}
