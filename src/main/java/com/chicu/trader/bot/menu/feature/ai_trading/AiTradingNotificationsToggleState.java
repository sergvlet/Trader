// AiTradingNotificationsToggleState.java
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
public class AiTradingNotificationsToggleState implements MenuState {

    private final AiTradingSettingsService svc;
    private final AiTradingDefaults defaults;
    private final InlineKeyboardMarkup kb;

    public AiTradingNotificationsToggleState(AiTradingSettingsService svc, AiTradingDefaults defaults) {
        this.svc = svc;
        this.defaults = defaults;

        InlineKeyboardButton toggle = InlineKeyboardButton.builder()
                .text("🔔/🔕 Вкл/Выкл").callbackData("notif_toggle").build();
        InlineKeyboardButton def    = InlineKeyboardButton.builder()
                .text("🔄 По умолчанию").callbackData("notif_default").build();
        InlineKeyboardButton save   = InlineKeyboardButton.builder()
                .text("💾 Сохранить").callbackData("notif_save").build();
        InlineKeyboardButton back   = InlineKeyboardButton.builder()
                .text("‹ Назад").callbackData("ai_trading_settings").build();

        this.kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(toggle),
                List.of(def),
                List.of(save),
                List.of(back)
            )).build();
    }

    @Override public String name() { return "ai_trading_settings_notifications_toggle"; }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings s = svc.getOrCreate(chatId);
        boolean cur = s.getNotificationsEnabled() != null 
            ? s.getNotificationsEnabled() : defaults.isDefaultNotificationsEnabled();
        String status = cur ? "Включены" : "Выключены";
        String text = String.format(
            "*Уведомления о сделках*\nТекущий статус: `%s`", status
        );
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text).parseMode("Markdown")
                .replyMarkup(kb).build();
    }

    @Override
    public String handleInput(Update u) {
        if (!u.hasCallbackQuery()) return name();
        String data = u.getCallbackQuery().getData();
        Long cid = u.getCallbackQuery().getMessage().getChatId();
        Boolean cur = svc.getOrCreate(cid).getNotificationsEnabled();
        if (cur == null) cur = defaults.isDefaultNotificationsEnabled();

        switch (data) {
            case "notif_toggle"   -> cur = !cur;
            case "notif_default"  -> { svc.resetNotificationsEnabledDefaults(cid); return name(); }
            case "notif_save"     -> { svc.updateNotificationsEnabled(cid, cur); return "ai_trading_settings"; }
            case "ai_trading_settings" -> { return "ai_trading_settings"; }
            default               -> { return name(); }
        }
        svc.updateNotificationsEnabled(cid, cur);
        return name();
    }
}
