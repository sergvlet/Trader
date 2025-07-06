package com.chicu.trader.bot.menu.feature.ai_trading.strategy.rsiema;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiRsiEmaStrategySettingsState implements MenuState {

    private final RsiEmaStrategySettingsService settingsService;

    @Override
    public String name() {
        return "ai_strategy_settings_rsi_ema";
    }

    @Override
    public SendMessage render(Long chatId) {
        RsiEmaStrategySettings cfg = settingsService.getOrCreate(chatId);

        String text = String.format("""
                *Настройки RSI/EMA*
                • RSI период: `%d`
                • EMA Short: `%d`
                • EMA Long: `%d`
                • Порог RSI BUY: `%.1f`
                • Порог RSI SELL: `%.1f`
                """, cfg.getRsiPeriod(), cfg.getEmaShort(), cfg.getEmaLong(),
                cfg.getRsiBuyThreshold(), cfg.getRsiSellThreshold());

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("🔧 RSI").callbackData("rsi_ema_edit:rsi").build(),
                                InlineKeyboardButton.builder().text("🔧 EMA").callbackData("rsi_ema_edit:ema").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("‹ Назад").callbackData("ai_trading_settings_strategy").build()
                        )
                ))
                .build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();

        String data = update.getCallbackQuery().getData();

        return switch (data) {
            case "rsi_ema_edit:rsi" -> "rsi_ema_config_rsi";
            case "rsi_ema_edit:ema" -> "rsi_ema_config_ema";
            case "ai_trading_settings_strategy" -> "ai_trading_settings_strategy";
            default -> name();
        };
    }
}
