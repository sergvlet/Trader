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

import java.util.ArrayList;
import java.util.List;

@Component("scalping_set_timeframe")
@RequiredArgsConstructor
public class ScalpingSetTimeframeState implements MenuState {

    private final ScalpingStrategySettingsService svc;
    private static final List<String> OPTIONS = List.of("1m","5m","15m","1h","4h","1d");

    @Override
    public String name() {
        return "scalping_set_timeframe";
    }

    @Override
    public SendMessage render(Long chatId) {
        String cur = svc.getOrCreate(chatId).getTimeframe();

        // соберём кнопки по две в ряд
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < OPTIONS.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = i; j < i + 2 && j < OPTIONS.size(); j++) {
                String tf = OPTIONS.get(j);
                row.add(InlineKeyboardButton.builder()
                    .text(tf.equals(cur) ? "✅ " + tf : tf)
                    .callbackData(name() + ":" + tf)
                    .build());
            }
            rows.add(row);
        }
        rows.add(List.of(
            InlineKeyboardButton.builder().text("‹ Назад").callbackData("scalping_config").build()
        ));

        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(String.format("*Timeframe:* `%s`", cur))
            .parseMode("Markdown")
            .replyMarkup(new InlineKeyboardMarkup(rows))
            .build();
    }

    @Override
    public @NonNull String handleInput(Update upd) {
        var cq = upd.getCallbackQuery();
        String data = cq.getData();
        Long chatId = cq.getMessage().getChatId();
        if ("scalping_config".equals(data)) {
            return "scalping_config";
        }
        if (data.startsWith(name() + ":")) {
            String tf = data.substring((name() + ":").length());
            var cfg = svc.getOrCreate(chatId);
            cfg.setTimeframe(tf);
            svc.save(cfg);
        }
        // всегда возвращаемся в главное меню
        return "scalping_config";
    }
}
