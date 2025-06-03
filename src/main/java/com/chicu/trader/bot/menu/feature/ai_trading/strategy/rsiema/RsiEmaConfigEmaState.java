package com.chicu.trader.bot.menu.feature.ai_trading.strategy.rsiema;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaStrategySettingsService;
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
public class RsiEmaConfigEmaState implements MenuState {

    private final RsiEmaStrategySettingsService strategySettingsService;

    @Override
    public String name() {
        return "rsi_ema_config_ema";
    }

    @Override
    public SendMessage render(Long chatId) {
        RsiEmaStrategySettings cfg = strategySettingsService.getOrCreate(chatId);

        String text = "*Настройка EMA*\n\n" +
                "EMA короткая: `" + cfg.getEmaShort() + "`\n" +
                "EMA длинная: `" + cfg.getEmaLong() + "`";

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➖").callbackData("ema_short_dec").build(),
                InlineKeyboardButton.builder().text("EMA SHORT").callbackData("noop").build(),
                InlineKeyboardButton.builder().text("➕").callbackData("ema_short_inc").build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➖").callbackData("ema_long_dec").build(),
                InlineKeyboardButton.builder().text("EMA LONG").callbackData("noop").build(),
                InlineKeyboardButton.builder().text("➕").callbackData("ema_long_inc").build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder().text("🔁 Сбросить").callbackData("rsi_ema_reset").build()
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
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        RsiEmaStrategySettings cfg = strategySettingsService.getOrCreate(chatId);

        switch (data) {
            case "ema_short_inc" -> cfg.setEmaShort(cfg.getEmaShort() + 1);
            case "ema_short_dec" -> cfg.setEmaShort(Math.max(1, cfg.getEmaShort() - 1));
            case "ema_long_inc" -> cfg.setEmaLong(cfg.getEmaLong() + 1);
            case "ema_long_dec" -> cfg.setEmaLong(Math.max(cfg.getEmaShort() + 1, cfg.getEmaLong() - 1));
            case "rsi_ema_reset" -> {
                strategySettingsService.resetEmaDefaults(chatId);
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
