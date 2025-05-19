// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingBacktestingState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AiTradingBacktestingState implements MenuState {

    private final AiTradingSettingsService settingsService;

    public AiTradingBacktestingState(AiTradingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public String name() {
        return "ai_trading_settings_backtesting";
    }

    @Override
    public SendMessage render(Long chatId) {
        InlineKeyboardButton runBtn = InlineKeyboardButton.builder()
                .text("▶️ Запустить бэктест")
                .callbackData("ai_trading_settings_backtesting:run")
                .build();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading_settings")
                .build();

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                    List.of(runBtn),
                    List.of(backBtn)
                ))
                .build();

        String text = "*Backtesting (бэктестинг)*\n" +
                      "Нажмите «Запустить бэктест», чтобы обучить модель, оптимизировать все параметры и включить AI-торговлю сразу.";
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();
        String data = u.getCallbackQuery().getData();
        Long chatId = u.getCallbackQuery().getMessage().getChatId();

        if ("ai_trading_settings_backtesting:run".equals(data)) {
            // Запускаем асинхронно бэктест + оптимизацию + включение торговли
            settingsService.trainAndApplyAsync(chatId);
            // После запуска возвращаемся в меню настроек
            return "ai_trading_settings";
        }
        if ("ai_trading_settings".equals(data)) {
            return "ai_trading_settings";
        }
        return name();
    }
}
