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

        List<ProfitablePair> allPairs = profitablePairService.getAllPairs(chatId);
        Set<String> activePairs = profitablePairService.getActivePairs(chatId)
                .stream().map(ProfitablePair::getSymbol).collect(Collectors.toSet());

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        for (ProfitablePair pair : allPairs) {
            boolean isActive = activePairs.contains(pair.getSymbol());
            buttons.add(List.of(
                    InlineKeyboardButton.builder()
                            .text((isActive ? "✅ " : "➖ ") + pair.getSymbol())
                            .callbackData("toggle:" + pair.getSymbol())
                            .build()
            ));
        }

        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("‹ Назад")
                        .callbackData(MenuService.BACK)
                        .build()
        ));

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("⚙ Выберите торговые пары:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
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
            String symbol = data.substring(7);
            profitablePairService.togglePair(chatId, symbol);

            // ⚠ БОЛЬШЕ НЕ НУЖНО: tradingExecutor.updateExecutor()
            // Executor подхватит изменения сам на следующем цикле
        }

        return name();
    }
}
