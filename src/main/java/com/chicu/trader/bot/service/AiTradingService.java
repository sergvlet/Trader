package com.chicu.trader.bot.service;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.TradingExecutor;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
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

    private final UserSettingsRepository userSettingsRepository;
    private final AiTradingSettingsService aiTradingSettingsService;
    private final AiTradingSettingsRepository aiSettingsRepo;
    private final ProfitablePairRepository pairRepo;
    private final TradingExecutor tradingExecutor;
    private final TradeLogRepository tradeLogRepository;
    private final StrategyRegistry strategyRegistry;

    private final Map<Long, Boolean> enabledMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        userSettingsRepository.findAll().forEach(us ->
                enabledMap.put(us.getChatId(), us.getAiTradingEnabled())
        );
        log.info("🔁 Загружено состояние AI-торговли: {} пользователей", enabledMap.size());
    }

    public boolean isTradingEnabled(Long chatId) {
        return enabledMap.getOrDefault(chatId, false);
    }

    @Transactional
    public void enableTrading(Long chatId) {
        log.info("▶️ Включение AI-торговли для chatId={}", chatId);

        UserSettings us = userSettingsRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found: " + chatId));
        us.setAiTradingEnabled(true);
        userSettingsRepository.saveAndFlush(us);
        enabledMap.put(chatId, true);

        log.info("✅ Обновлены настройки включения AI для chatId={}", chatId);

        aiTradingSettingsService.startAiTrading(chatId);
        log.info("🧠 Загружены настройки стратегии из базы для chatId={}", chatId);

        AiTradingSettings settings = aiSettingsRepo.findByUserChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("AiTradingSettings not found for chatId=" + chatId));

        TradeStrategy strategy = strategyRegistry.getByType(settings.getStrategy());

        if (strategy.isTrainable()) {
            log.info("📚 Стратегия {} требует обучения. Запуск train() для chatId={}", strategy.getType(), chatId);
            strategy.train(chatId);
            log.info("📘 Обучение завершено для chatId={}", chatId);
        } else {
            log.info("ℹ️ Стратегия {} не требует обучения для chatId={}", strategy.getType(), chatId);
        }

        List<String> symbols = pairRepo.findByUserChatIdAndActiveTrue(chatId).stream()
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.toList());

        log.info("📈 Получены активные пары для chatId={}: {}", chatId, symbols);

        tradingExecutor.startExecutor(chatId, symbols);

        log.info("✅ AI-торговля включена и запущена для chatId={}", chatId);
    }

    @Transactional
    public void disableTrading(Long chatId) {
        log.info("⏹ Отключение AI-торговли для chatId={}", chatId);

        UserSettings us = userSettingsRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found: " + chatId));
        us.setAiTradingEnabled(false);
        userSettingsRepository.saveAndFlush(us);
        enabledMap.put(chatId, false);

        tradingExecutor.stopExecutor();

        log.info("⛔ AI-торговля отключена для chatId={}", chatId);
    }

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
