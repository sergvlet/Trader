package com.chicu.trader.bot.menu.feature.ai_trading.strategy.scalping;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;
import com.chicu.trader.strategy.scalping.service.ScalpingStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("scalping_set_tp")
@RequiredArgsConstructor
public class ScalpingSetTpState implements MenuState {

    private final ScalpingStrategySettingsService svc;
    private static final double STEP = 0.5;

    @Override
    public String name() {
        return "scalping_set_tp";
    }

    @Override
    public SendMessage render(Long chatId) {
        ScalpingStrategySettings cfg = svc.getOrCreate(chatId);
        double v = cfg.getTakeProfitPct();

        var kb = InlineKeyboardMarkup.builder().keyboard(List.of(
            List.of(
                InlineKeyboardButton.builder().text("−").callbackData(name() + ":dec").build(),
                InlineKeyboardButton.builder().text("+").callbackData(name() + ":inc").build()
            ),
            List.of(
                InlineKeyboardButton.builder().text("‹ Назад").callbackData("scalping_config").build()
            )
        )).build();

        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(String.format("*Take profit:* `%.2f%%`", v))
            .parseMode("Markdown")
            .replyMarkup(kb)
            .build();
    }

    @Override
    public @NonNull String handleInput(Update upd) {
        String data = upd.getCallbackQuery().getData();
        Long chatId = upd.getCallbackQuery().getMessage().getChatId();
        if ("scalping_config".equals(data)) {
            return "scalping_config";
        }
        ScalpingStrategySettings cfg = svc.getOrCreate(chatId);
        double v = cfg.getTakeProfitPct();
        if (data.endsWith(":inc")) v += STEP;
        else if (data.endsWith(":dec")) v = Math.max(0, v - STEP);

        cfg.setTakeProfitPct(v);
        svc.save(cfg);

        return name();
    }
}
