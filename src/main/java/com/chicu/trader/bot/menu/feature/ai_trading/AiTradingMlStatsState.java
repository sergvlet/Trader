package com.chicu.trader.bot.menu.feature.ai_trading;

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiTradingMlStatsState implements MenuState {

    private final AiTradingSettingsService settingsService;

    @Override
    public String name() {
        return "ai_trading_stats";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);

        String date = s.getMlTrainedAt() == null
                ? "—"
                : Instant.ofEpochMilli(s.getMlTrainedAt())
                         .atZone(ZoneId.systemDefault())
                         .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String text = String.format("""
                📊 *Метрики модели:*

                Accuracy:  `%.4f`
                Precision: `%.4f`
                Recall:    `%.4f`
                AUC:       `%.4f`
                Обновлено: %s
                """,
                safe(s.getMlAccuracy()),
                safe(s.getMlPrecision()),
                safe(s.getMlRecall()),
                safe(s.getMlAuc()),
                date
        );

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text("‹ Назад")
                                        .callbackData("ai_trading_settings")
                                        .build()
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
        return "ai_trading_settings"; // возврат по кнопке назад
    }

    private double safe(Double val) {
        return val != null ? val : 0.0;
    }
}
