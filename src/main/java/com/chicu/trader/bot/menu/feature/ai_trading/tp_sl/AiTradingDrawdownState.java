package com.chicu.trader.bot.menu.feature.ai_trading.tp_sl;

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
public class AiTradingDrawdownState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup keyboard;

    public AiTradingDrawdownState(AiTradingSettingsService settingsService,
                                  AiTradingDefaults defaults) {
        this.settingsService = settingsService;
        this.defaults = defaults;

        InlineKeyboardButton inc  = InlineKeyboardButton.builder()
                .text("➕ Просадка")
                .callbackData("drawdown_inc")
                .build();
        InlineKeyboardButton dec  = InlineKeyboardButton.builder()
                .text("➖ Просадка")
                .callbackData("drawdown_dec")
                .build();
        InlineKeyboardButton def  = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию")
                .callbackData("drawdown_default")
                .build();
        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text("💾 Сохранить")
                .callbackData("drawdown_save")
                .build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData("drawdown_back")
                .build();

        this.keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                    List.of(inc, dec),
                    List.of(def),
                    List.of(save),
                    List.of(back)
                ))
                .build();
    }

    @Override
    public String name() {
        return "ai_trading_settings_drawdown";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = settingsService.getOrCreate(chatId);
        double dd = (s.getMaxDrawdown() != null)
                    ? s.getMaxDrawdown()
                    : defaults.getDefaultMaxDrawdown();
        String text = String.format(
            "*Максимальная просадка*\n"
          + "Бот остановит торговлю при просадке ≥ `%.1f%%`", dd
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
        String data   = update.getCallbackQuery().getData();
        Long   chatId = update.getCallbackQuery().getMessage().getChatId();

        // текущее или дефолтное значение
        Double dd = settingsService.getOrCreate(chatId).getMaxDrawdown();
        if (dd == null) {
            dd = defaults.getDefaultMaxDrawdown();
        }

        switch (data) {
            case "drawdown_inc"     -> dd = dd + 1.0;
            case "drawdown_dec"     -> dd = Math.max(1.0, dd - 1.0);
            case "drawdown_default" -> {
                settingsService.resetDrawdownDefaults(chatId);
                return name();
            }
            case "drawdown_save"    -> {
                settingsService.updateMaxDrawdown(chatId, dd);
                return "ai_trading_settings";
            }
            case "drawdown_back"    -> {
                return "ai_trading_settings";
            }
            default                 -> {
                return name();
            }
        }
        // при инкременте/декременте сразу сохраняем и перерисовываем
        settingsService.updateMaxDrawdown(chatId, dd);
        return name();
    }
}
