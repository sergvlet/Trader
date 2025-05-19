package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

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

        InlineKeyboardButton pairsBtn = InlineKeyboardButton.builder()
                .text("🔧 Изменить пары")
                .callbackData("ai_trading_settings_pairs")
                .build();

        InlineKeyboardButton topNBtn = InlineKeyboardButton.builder()
                .text("🔢 Кол-во пар")
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

        InlineKeyboardButton maxPosBtn = InlineKeyboardButton.builder()
                .text("🔀 Макс. позиций")
                .callbackData("ai_trading_settings_max_positions")
                .build();

        InlineKeyboardButton cooldownBtn = InlineKeyboardButton.builder()
                .text("⏳ Задержка между сделками")
                .callbackData("ai_trading_settings_trade_cooldown")
                .build();

        InlineKeyboardButton slippageBtn = InlineKeyboardButton.builder()
                .text("💧 Проскальзывание")
                .callbackData("ai_trading_settings_slippage_tolerance")
                .build();

        InlineKeyboardButton orderTypeBtn = InlineKeyboardButton.builder()
                .text("📋 Тип ордера")
                .callbackData("ai_trading_settings_order_type")
                .build();

        InlineKeyboardButton notificationsBtn = InlineKeyboardButton.builder()
                .text("🔔 Уведомления")
                .callbackData("ai_trading_settings_notifications_toggle")
                .build();

        InlineKeyboardButton modelVersionBtn = InlineKeyboardButton.builder()
                .text("🧠 Версия модели")
                .callbackData("ai_trading_settings_model_version")
                .build();

        InlineKeyboardButton leverageBtn = InlineKeyboardButton.builder()
                .text("📈 Плечо")
                .callbackData("ai_trading_settings_leverage")
                .build();
        InlineKeyboardButton backtestBtn = InlineKeyboardButton.builder()
                .text("🔬 Backtesting")
                .callbackData("ai_trading_settings_backtesting")
                .build();

        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("ai_trading")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(networkBtn),
                        List.of(tpSlBtn),
                        List.of(pairsBtn, topNBtn),
                        List.of(riskBtn, drawdownBtn),
                        List.of(timeframeBtn),
                        List.of(maxPosBtn, cooldownBtn),
                        List.of(slippageBtn, orderTypeBtn),
                        List.of(notificationsBtn, modelVersionBtn),
                        List.of(leverageBtn),
                        List.of(backtestBtn),
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
        return switch (data) {
            case "network_settings" -> "network_settings";
            case "ai_trading_settings_tp_sl" -> "ai_trading_settings_tp_sl";
            case "ai_trading_settings_pairs" -> "ai_trading_settings_pairs";
            case "ai_trading_settings_topn" -> "ai_trading_settings_topn";
            case "ai_trading_settings_risk" -> "ai_trading_settings_risk";
            case "ai_trading_settings_drawdown" -> "ai_trading_settings_drawdown";
            case "ai_trading_settings_timeframe" -> "ai_trading_settings_timeframe";
            case "ai_trading_settings_max_positions" -> "ai_trading_settings_max_positions";
            case "ai_trading_settings_trade_cooldown" -> "ai_trading_settings_trade_cooldown";
            case "ai_trading_settings_slippage_tolerance" -> "ai_trading_settings_slippage_tolerance";
            case "ai_trading_settings_order_type" -> "ai_trading_settings_order_type";
            case "ai_trading_settings_notifications_toggle" -> "ai_trading_settings_notifications_toggle";
            case "ai_trading_settings_model_version" -> "ai_trading_settings_model_version";
            case "ai_trading_settings_leverage" -> "ai_trading_settings_leverage";
            case "ai_trading_settings_backtesting" -> "ai_trading_settings_backtesting";
            case "ai_trading" -> MenuService.BACK;
            default -> name();
        };
    }
}
