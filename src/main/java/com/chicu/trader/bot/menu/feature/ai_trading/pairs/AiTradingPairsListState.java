package com.chicu.trader.bot.menu.feature.ai_trading.pairs;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.bot.service.BinancePairService;
import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.dto.BinancePairDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiTradingPairsListState implements MenuState {

    private final AiTradingSettingsService settingsService;
    private final UserSettingsService userSettingsService;
    private final BinancePairService binancePairService;

    private static final int PAGE_SIZE = 40;

    @Override
    public String name() {
        return "ai_trading_pairs_list";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        boolean isTestnet = "TEST".equalsIgnoreCase(userSettingsService.getMode(chatId));
        Set<String> selectedSymbols = new HashSet<>(Arrays.asList(settings.getSymbols().split(",")));

        int currentPage = settingsService.getCurrentPairPage(chatId);
        List<BinancePairDto> allPairs = binancePairService.getAllAvailablePairs(isTestnet);
        allPairs.sort(Comparator.comparingDouble(BinancePairDto::getPrice).reversed());

        int totalPages = (allPairs.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        List<BinancePairDto> pagePairs = allPairs.stream()
                .skip((long) currentPage * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки по 4 в ряд, только символ пары с галочкой
        for (int i = 0; i < pagePairs.size(); i += 4) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = 0; j < 4 && i + j < pagePairs.size(); j++) {
                BinancePairDto pair = pagePairs.get(i + j);
                boolean selected = selectedSymbols.contains(pair.getSymbol());
                String label = (selected ? "✅ " : "") + pair.getSymbol();
                row.add(InlineKeyboardButton.builder()
                        .text(label)
                        .callbackData("pair_toggle_" + pair.getSymbol())
                        .build());
            }
            rows.add(row);
        }

        // Навигация по страницам
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        if (currentPage > 0) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("⬅")
                    .callbackData("pair_page_" + (currentPage - 1))
                    .build());
        }
        navRow.add(InlineKeyboardButton.builder()
                .text("📄 " + (currentPage + 1) + "/" + totalPages)
                .callbackData("noop")
                .build());
        if (currentPage + 1 < totalPages) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("➡")
                    .callbackData("pair_page_" + (currentPage + 1))
                    .build());
        }
        if (!navRow.isEmpty()) {
            rows.add(navRow);
        }

        // Сохранить и Назад
        rows.add(List.of(
                InlineKeyboardButton.builder().text("💾 Сохранить").callbackData("pair_save").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("‹ Назад").callbackData("ai_trading_settings_pairs").build()
        ));

        // Формируем описание с ценой и ростом
        StringBuilder info = new StringBuilder();
        info.append("*📊 Выберите пары для AI торговли:*\n\n");
        if (selectedSymbols.isEmpty()) {
            info.append("_Пары не выбраны._\n\n");
        } else {
            info.append("Выбрано: `" + String.join(", ", selectedSymbols) + "`\n\n");
        }

        info.append("*Страница " + (currentPage + 1) + "/" + totalPages + "*\n");

        for (BinancePairDto pair : pagePairs) {
            String arrow = pair.getPriceChange() > 0 ? "📈" : pair.getPriceChange() < 0 ? "📉" : "➖";
            info.append(String.format("%s `%s` — `$%.2f` (%+.2f%%)\n",
                    arrow, pair.getSymbol(), pair.getPrice(), pair.getPriceChange()));
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(info.toString())
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) return name();
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        Set<String> selected = new HashSet<>(Arrays.asList(settings.getSymbols().split(",")));

        if (data.startsWith("pair_toggle_")) {
            String symbol = data.replace("pair_toggle_", "");
            if (selected.contains(symbol)) selected.remove(symbol);
            else selected.add(symbol);
            settingsService.updateSymbols(chatId, String.join(",", selected));
            return name(); // остаёмся на той же странице
        }

        if (data.startsWith("pair_page_")) {
            int page = Integer.parseInt(data.replace("pair_page_", ""));
            settingsService.setCurrentPairPage(chatId, page);
            return name(); // остаёмся на этом же состоянии, но страница меняется
        }

        if ("pair_save".equals(data)) return "ai_trading_settings";
        if ("ai_trading_settings_pairs".equals(data)) return "ai_trading_pairs";

        return name();
    }
}
