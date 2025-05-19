package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AiTradingOrderTypeState implements MenuState {

    private final AiTradingSettingsService svc;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup keyboard;

    public AiTradingOrderTypeState(AiTradingSettingsService svc, AiTradingDefaults defaults) {
        this.svc = svc;
        this.defaults = defaults;

        InlineKeyboardButton market          = InlineKeyboardButton.builder()
                .text("MARKET").callbackData("order_MARKET").build();
        InlineKeyboardButton limit           = InlineKeyboardButton.builder()
                .text("LIMIT").callbackData("order_LIMIT").build();
        InlineKeyboardButton stopLoss        = InlineKeyboardButton.builder()
                .text("STOP_LOSS").callbackData("order_STOP_LOSS").build();
        InlineKeyboardButton stopLossLimit   = InlineKeyboardButton.builder()
                .text("STOP_LOSS_LIMIT").callbackData("order_STOP_LOSS_LIMIT").build();
        InlineKeyboardButton takeProfit      = InlineKeyboardButton.builder()
                .text("TAKE_PROFIT").callbackData("order_TAKE_PROFIT").build();
        InlineKeyboardButton takeProfitLimit = InlineKeyboardButton.builder()
                .text("TAKE_PROFIT_LIMIT").callbackData("order_TAKE_PROFIT_LIMIT").build();
        InlineKeyboardButton limitMaker      = InlineKeyboardButton.builder()
                .text("LIMIT_MAKER").callbackData("order_LIMIT_MAKER").build();
        InlineKeyboardButton oco             = InlineKeyboardButton.builder()
                .text("OCO").callbackData("order_OCO").build();
        InlineKeyboardButton def             = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию").callbackData("order_default").build();
        InlineKeyboardButton save            = InlineKeyboardButton.builder()
                .text("💾 Сохранить").callbackData("order_save").build();
        InlineKeyboardButton back            = InlineKeyboardButton.builder()
                .text("‹ Назад").callbackData("ai_trading_settings").build();

        this.keyboard = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(market, limit),
                List.of(stopLoss, stopLossLimit),
                List.of(takeProfit, takeProfitLimit),
                List.of(limitMaker, oco),
                List.of(def),
                List.of(save),
                List.of(back)
        )).build();
    }

    @Override
    public String name() {
        return "ai_trading_settings_order_type";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = svc.getOrCreate(chatId);
        String current = s.getOrderType() != null
                ? s.getOrderType()
                : defaults.getDefaultOrderType();

        String text = String.join("\n",
            "*Тип ордера*\n" +
            "Выберите один из следующих типов (текущий: `" + current + "`):",
            "",
            "• *MARKET* — мгновенное исполнение по рынку",
            "• *LIMIT* — лимитный ордер по указанной цене",
            "• *STOP_LOSS* — стоп-рыночный ордер при достижении stopPrice",
            "• *STOP_LOSS_LIMIT* — стоп-лимитный ордер (stopPrice + limitPrice)",
            "• *TAKE_PROFIT* — тейк-профит-рыночный ордер",
            "• *TAKE_PROFIT_LIMIT* — тейк-профит-лимитный ордер",
            "• *LIMIT_MAKER* — лимитный ордер только на мейкер-сторону",
            "• *OCO* — стоп-лимит + тейк-профит в одном заказе (One-Cancels-the-Other)"
        );

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
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        String val = svc.getOrCreate(chatId).getOrderType();
        if (val == null) {
            val = defaults.getDefaultOrderType();
        }

        switch (data) {
            case "order_MARKET"            -> val = "MARKET";
            case "order_LIMIT"             -> val = "LIMIT";
            case "order_STOP_LOSS"         -> val = "STOP_LOSS";
            case "order_STOP_LOSS_LIMIT"   -> val = "STOP_LOSS_LIMIT";
            case "order_TAKE_PROFIT"       -> val = "TAKE_PROFIT";
            case "order_TAKE_PROFIT_LIMIT" -> val = "TAKE_PROFIT_LIMIT";
            case "order_LIMIT_MAKER"       -> val = "LIMIT_MAKER";
            case "order_OCO"               -> val = "OCO";
            case "order_default"           -> {
                svc.resetOrderTypeDefaults(chatId);
                return name();
            }
            case "order_save"              -> {
                svc.updateOrderType(chatId, val);
                return "ai_trading_settings";
            }
            case "ai_trading_settings"     -> {
                return "ai_trading_settings";
            }
            default                        -> {
                return name();
            }
        }

        // сразу сохраняем выбор и остаёмся в меню
        svc.updateOrderType(chatId, val);
        return name();
    }
}
