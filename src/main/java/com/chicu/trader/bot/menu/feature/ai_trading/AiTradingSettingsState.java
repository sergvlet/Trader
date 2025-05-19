package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Подменю «⚙️ Настройки» AI-режима:
 *  - Сетевые настройки
 *  - TP/SL настройки
 *  - Режим реинвестирования
 *  - Дополнительные настройки
 *  - Назад
 */
@Component
public class AiTradingSettingsState implements MenuState {

    private final InlineKeyboardMarkup keyboard;

    public AiTradingSettingsState() {
        InlineKeyboardButton networkBtn = InlineKeyboardButton.builder()
                .text("🌐 Сетевые настройки")
                .callbackData("network_settings")
                .build();

        InlineKeyboardButton tpSlBtn = InlineKeyboardButton.builder()
                .text("📈 TP/SL настройки")
                .callbackData("ai_trading_settings_tp_sl")
                .build();

        InlineKeyboardButton reinvestBtn = InlineKeyboardButton.builder()
                .text("🔄 Режим реинвестирования")
                .callbackData("ai_trading_settings_reinvest")
                .build();

        // Новые пункты
        InlineKeyboardButton pairsBtn = InlineKeyboardButton.builder()
                .text("🔧 Изменить пары")
                .callbackData("ai_trading_settings_pairs")
                .build();
        InlineKeyboardButton topNBtn = InlineKeyboardButton.builder()
                .text("🔢 Top N")
                .callbackData("ai_trading_settings_topn")
                .build();
        InlineKeyboardButton riskBtn = InlineKeyboardButton.builder()
                .text("⚠️ Риск")
                .callbackData("ai_trading_settings_risk")
                .build();
        InlineKeyboardButton drawdownBtn = InlineKeyboardButton.builder()
                .text("📉 Макс. просадка")
                .callbackData("ai_trading_settings_drawdown")
                .build();
        InlineKeyboardButton timeframeBtn = InlineKeyboardButton.builder()
                .text("⏱ Таймфрейм")
                .callbackData("ai_trading_settings_timeframe")
                .build();
        InlineKeyboardButton commissionBtn = InlineKeyboardButton.builder()
                .text("💰 Комиссия")
                .callbackData("ai_trading_settings_commission")
                .build();
        InlineKeyboardButton showAllBtn = InlineKeyboardButton.builder()
                .text("⚙️ Показать всё")
                .callbackData("ai_trading_settings_showall")
                .build();

        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(networkBtn),
                        List.of(tpSlBtn),
                        List.of(reinvestBtn),
                        List.of(pairsBtn, topNBtn),
                        List.of(riskBtn, drawdownBtn),
                        List.of(timeframeBtn, commissionBtn),
                        List.of(showAllBtn),
                        List.of(backBtn)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings";
    }

    @Override
    public SendMessage render(Long chatId) {
        String text = "*Настройки AI-торговли*\nВыберите пункт для изменения:";
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data = update.getCallbackQuery().getData();
        switch (data) {
            case "network_settings" -> {
                return "network_settings";
            }
            case "ai_trading_settings_tp_sl" -> {
                return "ai_trading_settings_tp_sl";
            }
            case "ai_trading_settings_reinvest" -> {
                return "ai_trading_settings_reinvest";
            }
            case "ai_trading_settings_pairs" -> {
                return "ai_trading_settings_pairs";
            }
            case "ai_trading_settings_topn" -> {
                return "ai_trading_settings_topn";
            }
            case "ai_trading_settings_risk" -> {
                return "ai_trading_settings_risk";
            }
            case "ai_trading_settings_drawdown" -> {
                return "ai_trading_settings_drawdown";
            }
            case "ai_trading_settings_timeframe" -> {
                return "ai_trading_settings_timeframe";
            }
            case "ai_trading_settings_commission" -> {
                return "ai_trading_settings_commission";
            }
            case "ai_trading_settings_showall" -> {
                return "ai_trading_settings_showall";
            }
            case "ai_trading" -> {
                return MenuService.BACK;
            }
            default -> {
                return name();
            }
        }
    }
}
