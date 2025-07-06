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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RsiEmaConfigRsiState implements MenuState {

    private final RsiEmaStrategySettingsService strategySettingsService;

    @Override
    public String name() {
        return "rsi_ema_config_rsi";
    }

    @Override
    public SendMessage render(Long chatId) {
        RsiEmaStrategySettings cfg = strategySettingsService.getOrCreate(chatId);

        String text = "*Настройка RSI*\n\n" +
                "Период: `" + cfg.getRsiPeriod() + "`\n" +
                "Порог BUY: `" + cfg.getRsiBuyThreshold() + "`\n" +
                "Порог SELL: `" + cfg.getRsiSellThreshold() + "`";

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➖").callbackData("rsi_period_dec").build(),
                InlineKeyboardButton.builder().text("Период RSI").callbackData("noop").build(),
                InlineKeyboardButton.builder().text("➕").callbackData("rsi_period_inc").build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➖").callbackData("rsi_buy_dec").build(),
                InlineKeyboardButton.builder().text("RSI BUY").callbackData("noop").build(),
                InlineKeyboardButton.builder().text("➕").callbackData("rsi_buy_inc").build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➖").callbackData("rsi_sell_dec").build(),
                InlineKeyboardButton.builder().text("RSI SELL").callbackData("noop").build(),
                InlineKeyboardButton.builder().text("➕").callbackData("rsi_sell_inc").build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder().text("🔁 Сбросить").callbackData("rsi_rsi_reset").build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder().text("‹ Назад").callbackData("rsi_ema_config").build()
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
    public @NonNull String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        RsiEmaStrategySettings cfg = strategySettingsService.getOrCreate(chatId);

        switch (data) {
            case "rsi_period_inc" -> cfg.setRsiPeriod(cfg.getRsiPeriod() + 1);
            case "rsi_period_dec" -> cfg.setRsiPeriod(Math.max(1, cfg.getRsiPeriod() - 1));
            case "rsi_buy_inc" -> cfg.setRsiBuyThreshold(cfg.getRsiBuyThreshold() + 1.0);
            case "rsi_buy_dec" -> cfg.setRsiBuyThreshold(cfg.getRsiBuyThreshold() - 1.0);
            case "rsi_sell_inc" -> cfg.setRsiSellThreshold(cfg.getRsiSellThreshold() + 1.0);
            case "rsi_sell_dec" -> cfg.setRsiSellThreshold(cfg.getRsiSellThreshold() - 1.0);
            case "rsi_rsi_reset" -> {
                strategySettingsService.resetRsiDefaults(chatId);
                return name();
            }
            case "rsi_ema_config" -> {
                return "rsi_ema_config";
            }
            default -> {
                return name();
            }
        }

        strategySettingsService.save(cfg);
        return name();
    }
}
