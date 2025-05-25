package com.chicu.trader.bot.menu.feature.ai_trading.strategy.rsiema;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.strategy.rsiema.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.RsiEmaStrategySettingsService;
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
public class RsiEmaConfigState implements MenuState {

    private final RsiEmaStrategySettingsService strategySettingsService;

    @Override
    public String name() {
        return "rsi_ema_config";
    }

    @Override
    public SendMessage render(Long chatId) {
        RsiEmaStrategySettings cfg = strategySettingsService.getOrCreate(chatId);

        String text = "*Настройка RSI + EMA*\n\n"
                + "• RSI период: `" + cfg.getRsiPeriod() + "`\n"
                + "• EMA короткая: `" + cfg.getEmaShort() + "`\n"
                + "• EMA длинная: `" + cfg.getEmaLong() + "`\n"
                + "• RSI BUY-порог: `" + cfg.getRsiBuyThreshold() + "`\n"
                + "• RSI SELL-порог: `" + cfg.getRsiSellThreshold() + "`";

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("📈 Настройки RSI").callbackData("rsi_ema_config_rsi").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("📉 Настройки EMA").callbackData("rsi_ema_config_ema").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("‹ Назад").callbackData("ai_trading_settings_strategy").build()
        ));

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder().keyboard(rows).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();
        String data = update.getCallbackQuery().getData();

        return switch (data) {
            case "rsi_ema_config_rsi" -> "rsi_ema_config_rsi";
            case "rsi_ema_config_ema" -> "rsi_ema_config_ema";
            case "ai_trading_settings_strategy" -> "ai_trading_settings_strategy";
            default -> name();
        };
    }
}
