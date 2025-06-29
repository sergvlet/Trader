package com.chicu.trader.bot.menu.feature.ai_trading.pairs;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component("ai_trading_pairs_manual")
@RequiredArgsConstructor
public class AiTradingPairsManualState implements MenuState {

    private final AiTradingSettingsService aiTradingSettingsService;
    private final ProfitablePairService profitablePairService;

    @Override
    public String name() {
        return "ai_trading_pairs_manual";
    }

    @Override
    public SendMessage render(Long chatId) {
        // Получаем все пары и активные символы
        List<ProfitablePair> allPairs = profitablePairService.getAllPairs(chatId);
        Set<String> activeSymbols = profitablePairService.getActivePairs(chatId)
                .stream().map(ProfitablePair::getSymbol).collect(Collectors.toSet());

        // Строим кнопки: для каждой пары – своя строка
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (ProfitablePair pair : allPairs) {
            boolean active = activeSymbols.contains(pair.getSymbol());
            rows.add(List.of(
                    InlineKeyboardButton.builder()
                            .text((active ? "✅ " : "➖ ") + pair.getSymbol())
                            .callbackData("toggle:" + pair.getSymbol())
                            .build()
            ));
        }

        // Кнопка «Назад»
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("‹ Назад")
                        .callbackData(MenuService.BACK)
                        .build()
        ));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("⚙ Выберите торговые пары:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }

        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        if (data.startsWith("toggle:")) {
            String symbol = data.substring("toggle:".length());
            // Переключаем статус пары в БД
            profitablePairService.togglePair(chatId, symbol);
            // остаёмся в том же состоянии, чтобы сразу увидеть обновлённый список
            return name();
        }

        // Меню-сервис обработает MenuService.BACK и вернёт предыдущее состояние
        return data;
    }
}
