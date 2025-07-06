package com.chicu.trader.bot.menu.feature.ai_trading.tp_sl;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.config.AiTradingDefaults;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class AiTradingTpSlState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults defaults;
    private final ObjectMapper objectMapper;
    private final InlineKeyboardMarkup keyboard;

    public AiTradingTpSlState(AiTradingSettingsService settingsService,
                              AiTradingDefaults defaults) {
        this.settingsService = settingsService;
        this.defaults = defaults;
        this.objectMapper = new ObjectMapper();

        InlineKeyboardButton incTp = InlineKeyboardButton.builder()
                .text("➕ TP")
                .callbackData("tp_inc")
                .build();
        InlineKeyboardButton decTp = InlineKeyboardButton.builder()
                .text("➖ TP")
                .callbackData("tp_dec")
                .build();
        InlineKeyboardButton incSl = InlineKeyboardButton.builder()
                .text("➕ SL")
                .callbackData("sl_inc")
                .build();
        InlineKeyboardButton decSl = InlineKeyboardButton.builder()
                .text("➖ SL")
                .callbackData("sl_dec")
                .build();
        InlineKeyboardButton def    = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию")
                .callbackData("tp_sl_default")
                .build();
        InlineKeyboardButton save   = InlineKeyboardButton.builder()
                .text("💾 Сохранить")
                .callbackData("tp_sl_save")
                .build();
        InlineKeyboardButton back   = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("tp_sl_back")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(incTp, decTp),
                        List.of(incSl, decSl),
                        List.of(def),
                        List.of(save),
                        List.of(back)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings_tp_sl";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        double tp = 0, sl = 0;
        try {
            JsonNode node = objectMapper.readTree(s.getTpSlConfig());
            tp = node.path("tp").asDouble();
            sl = node.path("sl").asDouble();
        } catch (Exception ignore) { }

        // Добавили описание сразу перед показом текущих значений
        String text = String.join("\n",
            "🎯 *Take-Profit* — целевая цена для фиксации прибыли",
            "🚨 *Stop-Loss*   — цена срабатывания стоп-лосса для ограничения убытков",
            "",
            String.format("*TP/SL Настройки:*\nTP: `%.2f%%`\nSL: `%.2f%%`", tp * 100, sl * 100),
            "",
            "Выберите действие:"
        );

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update update) {
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();
        AiTradingSettings s = settingsService.getOrCreate(chatId);

        double tp = 0, sl = 0;
        try {
            JsonNode node = objectMapper.readTree(s.getTpSlConfig());
            tp = node.path("tp").asDouble();
            sl = node.path("sl").asDouble();
        } catch (Exception ignore) { }

        switch (data) {
            case "tp_inc"         -> tp += 0.01;
            case "tp_dec"         -> tp = Math.max(0.0, tp - 0.01);
            case "sl_inc"         -> sl += 0.01;
            case "sl_dec"         -> sl = Math.max(0.0, sl - 0.01);
            case "tp_sl_default"  -> {
                // сброс к дефолтным
                tp = defaults.getDefaultTp();
                sl = defaults.getDefaultSl();
                saveTpSl(chatId, tp, sl);
                return name(); // остаёмся в меню TP/SL
            }
            case "tp_sl_save"     -> {
                saveTpSl(chatId, tp, sl);
                return "ai_trading_settings"; // возвращаемся в общее меню настроек
            }
            case "tp_sl_back"     -> {
                return "ai_trading_settings"; // просто назад
            }
            default               -> {
                return name();
            }
        }

        // при изменении TP/SL (inc/dec) сразу сохраняем и перерисовываем текущее меню
        saveTpSl(chatId, tp, sl);
        return name();
    }

    private void saveTpSl(Long chatId, double tp, double sl) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("tp", tp);
        node.put("sl", sl);
        settingsService.updateTpSl(chatId, node.toString());
    }
}
