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

@Component("scalping_set_candles")
@RequiredArgsConstructor
public class ScalpingSetCandlesState implements MenuState {

    private final ScalpingStrategySettingsService svc;
    private static final int STEP = 50;

    @Override
    public String name() {
        return "scalping_set_candles";
    }

    @Override
    public SendMessage render(Long chatId) {
        ScalpingStrategySettings cfg = svc.getOrCreate(chatId);
        int v = cfg.getCachedCandlesLimit();

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
            .text(String.format("*Candles cache limit:* `%d`", v))
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
        int v = cfg.getCachedCandlesLimit();
        if (data.endsWith(":inc")) v += STEP;
        else if (data.endsWith(":dec")) v = Math.max(STEP, v - STEP);

        cfg.setCachedCandlesLimit(v);
        svc.save(cfg);

        return name();
    }
}
