// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/strategy/rsiema/RsiEmaConfigState.java
package com.chicu.trader.bot.menu.feature.ai_trading.strategy.rsiema;


import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaStrategySettingsService;
import com.chicu.trader.bot.menu.core.MenuState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    private final RsiEmaStrategySettingsService rsiEmaSettingsService;
    private final ApplicationEventPublisher     eventPublisher;

    @Override
    public String name() {
        return "rsi_ema_config";
    }

    @Override
    public SendMessage render(Long chatId) {
        RsiEmaStrategySettings cfg = rsiEmaSettingsService.getOrCreate(chatId);
        String text = String.format(
                "*Настройки RSI + EMA*\n\n" +
                        "RSI period: %d\n" +
                        "Buy thresh: %.2f\n" +
                        "Sell thresh: %.2f\n" +
                        "EMA short: %d\n" +
                        "EMA long: %d\n\n" +
                        "Выберите действие:",
                cfg.getRsiPeriod(),
                cfg.getRsiBuyThreshold(),
                cfg.getRsiSellThreshold(),
                cfg.getEmaShort(),
                cfg.getEmaLong()
        );

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("⚙️ RSI")
                        .callbackData("rsi_ema_config_rsi")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("📉 EMA")
                        .callbackData("rsi_ema_config_ema")
                        .build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("📊 Показать график")
                        .callbackData("rsi_ema_show_chart")
                        .build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("‹ Назад")
                        .callbackData("ai_trading_settings")
                        .build()
        ));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(new InlineKeyboardMarkup(rows))
                .build();
    }

    @Override
    public @NonNull String handleInput(Update upd) {
        if (!upd.hasCallbackQuery()) {
            return name();
        }
        var cq = upd.getCallbackQuery();
        String data = cq.getData();
        Long chatId = cq.getMessage().getChatId();
        Integer messageId = cq.getMessage().getMessageId();

        return switch (data) {
            case "rsi_ema_config_rsi" ->
                    "rsi_ema_config_rsi";
            case "rsi_ema_config_ema" ->
                    "rsi_ema_config_ema";
            case "ai_trading_settings"   -> "ai_trading_settings";
            default -> name();
        };
    }

}
