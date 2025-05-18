// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.TradingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.stream.Collectors;

@Component("ai_trading")
@RequiredArgsConstructor
public class AiTradingState implements MenuState {

    private final TradingStatusService statusService;
    private final ProfitablePairRepository pairRepo;

    @Override
    public String name() {
        return "ai_trading";
    }

    @Override
    public SendMessage render(Long chatId) {
        boolean running = statusService.isRunning(chatId);

        // Строка статуса торговли
        String statusLine = running
                ? "▶️ *Торговля запущена!*"
                : "❌ *Торговля остановлена*";

        // Список пар
        List<ProfitablePair> pairs = pairRepo.findByUserChatIdAndActiveTrue(chatId);
        String pairsLine = pairs.isEmpty()
                ? "_Нет активных пар_"
                : "_Пары_: " + pairs.stream()
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.joining(", "));

        // Последнее событие
        String last = statusService.getLastEvent(chatId);
        String lastLine = (last == null || last.isBlank())
                ? "_Последнее событие: нет данных_"
                : "*Последнее событие:* " + last;

        // Собираем текст
        String text = String.join("\n",
                "*AI-торговля*",
                statusLine,
                pairsLine,
                lastLine,
                "",
                "Выберите действие:"
        );

        // Кнопки
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text(running ? "▶️ Запустить снова" : "▶️ Начать торговлю")
                                        .callbackData("ai_trading:start").build(),
                                InlineKeyboardButton.builder()
                                        .text(running ? "⏹️ Остановить торговлю" : "⏹️ Остановлена")
                                        .callbackData("ai_trading:stop").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text("⚙️ Настройки")
                                        .callbackData("ai_trading:settings").build(),
                                InlineKeyboardButton.builder()
                                        .text("📊 Статистика")
                                        .callbackData("ai_trading:statistics").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text("📋 Ордера")
                                        .callbackData("ai_trading:orders").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text("‹ Назад")
                                        .callbackData(MenuService.BACK).build()
                        )
                ))
                .build();

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
        return switch (data) {
            case "ai_trading:start", "ai_trading:stop"   -> name();
            case "ai_trading:settings"                   -> "ai_trading_settings";
            case "ai_trading:statistics"                 -> "ai_trading_statistics";
            case "ai_trading:orders"                     -> "ai_trading_orders";
            case MenuService.BACK                        -> MenuService.BACK;
            default                                      -> name();
        };
    }
}
