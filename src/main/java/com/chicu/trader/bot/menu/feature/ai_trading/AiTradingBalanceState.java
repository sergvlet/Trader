package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.trading.service.binance.client.BinanceHttpClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTradingBalanceState implements MenuState {

    private final UserSettingsService userSettingsService;
    private final BinanceRestClientFactory restClientFactory;

    private static final int PAGE_SIZE = 20;

    @Override
    public String name() {
        return "ai_trading_settings_balance";
    }

    @Override
    public SendMessage render(Long chatId) {
        try {
            boolean isTest = userSettingsService.isTestnet(chatId);
            BinanceRestClient client = restClientFactory.getClient(chatId);
            Map<String, BinanceHttpClient.BalanceInfo> balanceMap = client.getFullBalance();

            var balances = balanceMap.entrySet().stream()
                    .filter(e -> e.getValue().getTotal().doubleValue() > 0.0)
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            int page = MenuUtils.getPage(chatId);
            int totalPages = (int) Math.ceil((double) balances.size() / PAGE_SIZE);
            int fromIndex = Math.min(page * PAGE_SIZE, balances.size());
            int toIndex = Math.min(fromIndex + PAGE_SIZE, balances.size());

            List<Map.Entry<String, BinanceHttpClient.BalanceInfo>> pageBalances = balances.subList(fromIndex, toIndex);

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (int i = 0; i < pageBalances.size(); i += 3) {
                List<InlineKeyboardButton> row = pageBalances.subList(i, Math.min(i + 3, pageBalances.size()))
                        .stream()
                        .map(e -> InlineKeyboardButton.builder()
                                .text(e.getKey())
                                .callbackData("ai_trading_balance_asset:" + e.getKey())
                                .build())
                        .toList();
                rows.add(row);
            }

            // Навигационные стрелки
            List<InlineKeyboardButton> navRow = new ArrayList<>();
            if (page > 0) {
                navRow.add(InlineKeyboardButton.builder().text("⏪ Назад").callbackData("ai_trading_balance_prev").build());
            }
            if (toIndex < balances.size()) {
                navRow.add(InlineKeyboardButton.builder().text("⏩ Вперёд").callbackData("ai_trading_balance_next").build());
            }
            if (!navRow.isEmpty()) rows.add(navRow);

            // Кнопка Назад
            rows.add(List.of(MenuUtils.backButton("ai_trading_settings")));

            var kb = InlineKeyboardMarkup.builder().keyboard(rows).build();
            var text = "*💰 Монеты на аккаунте (%s режим):*\nВсего: %d"
                    .formatted(isTest ? "тестовый" : "реальный", balances.size());

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .replyMarkup(kb)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении баланса", e);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("❌ Не удалось получить баланс: " + e.getMessage())
                    .replyMarkup(MenuUtils.backKeyboard("ai_trading_settings"))
                    .build();
        }
    }

    @Override
    public String handleInput(Update update) {
        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.equals("ai_trading_balance_next")) {
            MenuUtils.incrementPage(chatId);
            return name();
        }
        if (data.equals("ai_trading_balance_prev")) {
            MenuUtils.decrementPage(chatId);
            return name();
        }

        if (data.startsWith("ai_trading_balance_asset:")) {
            String asset = data.substring(data.indexOf(":") + 1);
            MenuUtils.setSelectedAsset(chatId, asset);
            return "ai_trading_balance_asset";
        }

        return name();
    }
}
