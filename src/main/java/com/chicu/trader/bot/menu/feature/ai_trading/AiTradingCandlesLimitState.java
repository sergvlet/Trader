package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
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
public class AiTradingCandlesLimitState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults defaults;

    @Override
    public String name() {
        return "ai_trading_settings_cached_candles_limit";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        int current = s.getCachedCandlesLimit() != null
                ? s.getCachedCandlesLimit()
                : defaults.getDefaultCachedCandlesLimit();

        String text = String.join("\n",
                "📊 *Кол-во свечей для кэширования*",
                "",
                "*Текущее значение:* `" + current + "`",
                "",
                "Выберите действие:"
        );

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("➕ Увеличить").callbackData("candles_inc").build(),
                                InlineKeyboardButton.builder().text("➖ Уменьшить").callbackData("candles_dec").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("🔄 По умолчанию").callbackData("candles_default").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("💾 Сохранить").callbackData("candles_save").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("‹ Назад").callbackData("candles_back").build()
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
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        AiTradingSettings s = settingsService.getOrCreate(chatId);

        int value = s.getCachedCandlesLimit() != null
                ? s.getCachedCandlesLimit()
                : defaults.getDefaultCachedCandlesLimit();

        switch (data) {
            case "candles_inc" -> value = Math.min(5000, value + 50);
            case "candles_dec" -> value = Math.max(50, value - 50);
            case "candles_default" -> value = defaults.getDefaultCachedCandlesLimit();
            case "candles_save" -> {
                settingsService.updateCachedCandlesLimit(chatId, value);
                return "ai_trading_settings";
            }
            case "candles_back" -> {
                return "ai_trading_settings";
            }
        }

        s.setCachedCandlesLimit(value);
        settingsService.save(s);

        return name(); // остаёмся в меню
    }
}
