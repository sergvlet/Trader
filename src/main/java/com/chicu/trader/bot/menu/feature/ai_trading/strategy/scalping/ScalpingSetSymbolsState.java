package com.chicu.trader.bot.menu.feature.ai_trading.strategy.scalping;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.strategy.scalping.service.ScalpingStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("scalping_set_symbols")
@RequiredArgsConstructor
public class ScalpingSetSymbolsState implements MenuState {

    private final ScalpingStrategySettingsService svc;

    @Override
    public String name() {
        return "scalping_set_symbols";
    }

    @Override
    public SendMessage render(Long chatId) {
        String cur = svc.getOrCreate(chatId).getSymbol();
        String text = "*Symbols:*\n" +
            (cur.isBlank()
                ? "_не заданы_\n\nВведите список через запятую"
                : String.format("`%s`\n\nВведите новый список через запятую", cur)
            );

        var kb = InlineKeyboardMarkup.builder().keyboard(List.of(
            List.of(
                InlineKeyboardButton.builder()
                    .text("‹ Назад")
                    .callbackData("scalping_config")
                    .build()
            )
        )).build();

        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(kb)
            .build();
    }

    @Override
    public @NonNull String handleInput(Update upd) {

        String text = upd.getMessage().getText().trim();
        Long chatId = upd.getMessage().getChatId();
        if (upd.hasCallbackQuery()) {
            return "scalping_config";
        }
        var cfg = svc.getOrCreate(chatId);
        cfg.setSymbol(text);
        svc.save(cfg);
        return "scalping_config";
    }
}
