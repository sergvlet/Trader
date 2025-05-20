// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingService;
import com.chicu.trader.trading.TradingExecutor;
import com.chicu.trader.trading.service.binance.HttpBinanceWebSocketService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
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

    private final AiTradingService                 aiService;
    private final ProfitablePairRepository         pairRepo;
    private final HttpBinanceWebSocketService      webSocketService;
    private final TradingExecutor tradingExecutor;

    @Override
    public String name() {
        return "ai_trading";
    }

    @Override
    public SendMessage render(Long chatId) {
        boolean running = aiService.isTradingEnabled(chatId);
        String statusLine = running
            ? "▶️ *Торговля запущена!*"
            : "❌ *Торговля остановлена*";

        List<ProfitablePair> pairs = pairRepo.findByUserChatIdAndActiveTrue(chatId);
        String pairsLine = pairs.isEmpty()
            ? "_Нет активных пар_"
            : "_Пары_: " + pairs.stream()
                                 .map(ProfitablePair::getSymbol)
                                 .collect(Collectors.joining(", "));

        String lastEvt = aiService.getLastEvent(chatId);
        String lastLine = (lastEvt == null || lastEvt.isBlank())
            ? "_Последнее событие: нет данных_"
            : "*Последнее событие:* " + lastEvt;

        String text = String.join("\n",
            "*AI-торговля*",
            statusLine,
            pairsLine,
            lastLine,
            "",
            "Выберите действие:"
        );

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(running ? "▶️ Запустить снова" : "▶️ Начать торговлю")
                        .callbackData("ai_trading:start")
                        .build(),
                    InlineKeyboardButton.builder()
                        .text(running ? "⏹️ Остановить торговлю" : "⏹️ Остановлена")
                        .callbackData("ai_trading:stop")
                        .build()
                ),
                List.of(
                    InlineKeyboardButton.builder()
                        .text("⚙️ Настройки")
                        .callbackData("ai_trading:settings")
                        .build(),
                    InlineKeyboardButton.builder()
                        .text("📊 Статистика")
                        .callbackData("ai_trading:statistics")
                        .build()
                ),
                List.of(
                    InlineKeyboardButton.builder()
                        .text("📋 Ордера")
                        .callbackData("ai_trading:orders")
                        .build()
                ),
                List.of(
                    InlineKeyboardButton.builder()
                        .text("‹ Назад")
                        .callbackData(MenuService.BACK)
                        .build()
                )
            ))
            .build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        switch (data) {
            case "ai_trading:start" -> {
                aiService.enableTrading(chatId);
                List<String> symbols = pairRepo.findByUserChatIdAndActiveTrue(chatId)
                        .stream()
                        .map(ProfitablePair::getSymbol)
                        .collect(Collectors.toList());
                tradingExecutor.startExecutor(chatId, symbols);
                return name();
            }
            case "ai_trading:stop" -> {
                aiService.disableTrading(chatId);
                tradingExecutor.stopExecutor();
                return name();
            }
            case "ai_trading:settings"    -> { return "ai_trading_settings"; }
            case "ai_trading:statistics"  -> { return "ai_trading_statistics"; }
            case "ai_trading:orders"      -> { return "ai_trading_orders"; }
            case MenuService.BACK         -> { return MenuService.BACK; }
            default                       -> { return name(); }
        }
    }
}
