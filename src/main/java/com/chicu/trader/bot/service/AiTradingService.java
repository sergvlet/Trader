// src/main/java/com/chicu/trader/bot/service/AiTradingService.java
package com.chicu.trader.bot.service;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.TradingExecutor;
import com.chicu.trader.trading.repository.TradeLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTradingService {

    private final UserSettingsRepository       userSettingsRepository;
    private final AiTradingSettingsService     aiTradingSettingsService;
    private final ProfitablePairRepository     pairRepo;
    private final TradingExecutor              tradingExecutor;
    private final TradeLogRepository           tradeLogRepository;

    private final Map<Long, Boolean> enabledMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        userSettingsRepository.findAll().forEach(us ->
                enabledMap.put(us.getChatId(), us.getAiTradingEnabled())
        );
        log.info("Загружено состояние AI-торговли: {} пользователей", enabledMap.size());
    }

    public boolean isTradingEnabled(Long chatId) {
        return enabledMap.getOrDefault(chatId, false);
    }

    @Transactional
    public void enableTrading(Long chatId) {
        UserSettings us = userSettingsRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found: " + chatId));
        us.setAiTradingEnabled(true);
        userSettingsRepository.saveAndFlush(us);
        enabledMap.put(chatId, true);

        aiTradingSettingsService.startAiTrading(chatId);

        // Получаем список активных символов из таблицы profitable_pairs
        List<String> symbols = pairRepo.findByUserChatIdAndActiveTrue(chatId).stream()
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.toList());

        tradingExecutor.startExecutor(chatId, symbols);

        log.info("✅ AI-торговля включена для chatId={}", chatId);
    }

    @Transactional
    public void disableTrading(Long chatId) {
        UserSettings us = userSettingsRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found: " + chatId));
        us.setAiTradingEnabled(false);
        userSettingsRepository.saveAndFlush(us);
        enabledMap.put(chatId, false);

        tradingExecutor.stopExecutor();

        log.info("⛔ AI-торговля отключена для chatId={}", chatId);
    }

    /**
     * Возвращает краткое описание последней сделки пользователя.
     */
    public String getLastEvent(Long chatId) {
        Optional<com.chicu.trader.model.TradeLog> last = tradeLogRepository
                .findTopByUserChatIdOrderByEntryTimeDesc(chatId);
        if (last.isEmpty()) {
            return "Сделок пока не было";
        }
        com.chicu.trader.model.TradeLog tl = last.get();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm");

        java.time.ZonedDateTime entryZdt = java.time.ZonedDateTime.ofInstant(tl.getEntryTime(), java.time.ZoneId.systemDefault());
        String entryTime = fmt.format(entryZdt);

        if (!tl.isClosed()) {
            return String.format(
                    "Открыта позиция %s\nв %s по %.4f\nTP %.4f  SL %.4f",
                    tl.getSymbol(), entryTime, tl.getEntryPrice(),
                    tl.getTakeProfitPrice(), tl.getStopLossPrice()
            );
        } else {
            java.time.ZonedDateTime exitZdt = java.time.ZonedDateTime.ofInstant(tl.getExitTime(), java.time.ZoneId.systemDefault());
            String exitTime = fmt.format(exitZdt);
            double pnl = tl.getPnl();
            return String.format(
                    "Сделка %s: вход %.4f → выход %.4f\nPnL: %.4f\n(%s)",
                    tl.getSymbol(), tl.getEntryPrice(), tl.getExitPrice(),
                    pnl, exitTime
            );
        }
    }
}
