package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.bot.test.TestTradeLogSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiTrainingTestTrainState implements MenuState {

    private final AiTradingSettingsService settingsService;

    @Autowired
    private TestTradeLogSeeder testTradeLogSeeder;

    @Override
    public String name() {
        return "ai_training_test_train";
    }

    @Override
    public SendMessage render(Long chatId) {
        testTradeLogSeeder.seedLogs(chatId);                  // ✅ генерируем данные
        settingsService.trainAndApplyAsync(chatId);           // ✅ запускаем обучение

        String text = "🔁 Обучение запущено ✅\n" +
                      "Добавлены тестовые сделки.\n\n" +
                      "Метрики будут доступны в 📊 ML-метрики.";

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(InlineKeyboardButton.builder()
                                .text("‹ Назад")
                                .callbackData("ai_trading_settings")
                                .build())
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
    public String handleInput(Update update) {
        return "ai_trading_settings";
    }
}
