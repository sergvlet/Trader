package com.chicu.trader.bot.service;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.bot.repository.UserRepository;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.ml.MlModelTrainer;
import com.chicu.trader.trading.ml.MlTrainingException;
import com.chicu.trader.trading.ml.MlTrainingMetrics;
import com.chicu.trader.trading.optimizer.OptimizerService;
import com.chicu.trader.trading.repository.ProfitablePairRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTradingSettingsService {

    private final AiTradingSettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final AiTradingDefaults defaults;
    private final ProfitablePairRepository pairRepo;
    private final MlModelTrainer modelTrainer;
    private final OptimizerService optimizer;

    /** === Создаём или загружаем === */
    @Transactional
    public AiTradingSettings getOrCreate(Long chatId) {
        return settingsRepo.findById(chatId).orElseGet(() -> {
            User user = userRepo.findById(chatId)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + chatId));
            AiTradingSettings s = AiTradingSettings.builder()
                    .user(user)
                    .networkMode(defaults.getNetworkMode())
                    .tpSlConfig("{}")
                    .reinvestEnabled(defaults.isDefaultReinvest())
                    .symbols(defaults.getDefaultSymbols())
                    .topN(defaults.getDefaultTopN())
                    .riskThreshold(defaults.getDefaultRiskThreshold())
                    .maxDrawdown(defaults.getDefaultMaxDrawdown())
                    .timeframe(defaults.getDefaultTimeframe())
                    .commission(defaults.getDefaultCommission())
                    .leverage(defaults.getDefaultLeverage())
                    .maxPositions(defaults.getDefaultMaxPositions())
                    .tradeCooldown(defaults.getDefaultTradeCooldown())
                    .slippageTolerance(defaults.getDefaultSlippageTolerance())
                    .orderType(defaults.getDefaultOrderType())
                    .notificationsEnabled(defaults.isDefaultNotificationsEnabled())
                    .modelVersion(defaults.getDefaultModelVersion())
                    .cachedCandlesLimit(defaults.getDefaultCachedCandlesLimit())
                    .strategy(StrategyType.valueOf(defaults.getDefaultStrategy()))
                    .build();
            log.info("Созданы настройки AI для chatId={}", chatId);
            return settingsRepo.save(s);
        });
    }

    /** === Основное обучение + оптимизация === */
    @Async("mlExecutor")
    public CompletableFuture<Void> trainAndApplyAsync(Long chatId) {
        log.info("🔬 Запуск обучения и оптимизации для chatId={}", chatId);

        String modelPath = String.format("models/%d/ml_signal_filter.onnx", chatId);
        final MlTrainingMetrics metrics;
        try {
            metrics = modelTrainer.trainAndExport(chatId, modelPath);
        } catch (MlTrainingException e) {
            log.error("❌ Не удалось обучить ML-модель для chatId={}", chatId, e);
            return CompletableFuture.failedFuture(e);
        }

        // прогоняем оптимизацию
        optimizer.optimizeAllForChat(chatId);

        // сохраняем метрики в настройках
        AiTradingSettings s = getOrCreate(chatId);
        s.setMlAccuracy(metrics.getAccuracy());
        s.setMlAuc(metrics.getAuc());
        s.setMlPrecision(metrics.getPrecision());
        s.setMlRecall(metrics.getRecall());
        s.setMlTrainedAt(System.currentTimeMillis());
        settingsRepo.save(s);

        log.info("✅ Модель обучена и оптимизирована для chatId={}, acc={}, auc={}",
                chatId, metrics.getAccuracy(), metrics.getAuc());
        return CompletableFuture.completedFuture(null);
    }

    // === Методы обновления параметров ===

    public void updateRiskThreshold(Long chatId, Double riskPercent) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setRiskThreshold(riskPercent);
        settingsRepo.save(s);
    }

    public void updateSymbols(Long chatId, String symbolsCsv) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setSymbols(symbolsCsv);
        settingsRepo.save(s);
    }

    public void updateTimeframe(Long chatId, String timeframe) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTimeframe(timeframe);
        settingsRepo.save(s);
    }

    public void updateTopN(Long chatId, Integer topN) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTopN(topN);
        settingsRepo.save(s);
    }

    public void updateLeverage(Long chatId, Integer leverage) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setLeverage(leverage);
        settingsRepo.save(s);
    }

    public void updateMaxDrawdown(Long chatId, Double value) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setMaxDrawdown(value);
        settingsRepo.save(s);
    }

    public void updateTradeCooldown(Long chatId, Integer cooldown) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTradeCooldown(cooldown);
        settingsRepo.save(s);
    }

    public void updateSlippageTolerance(Long chatId, Double slippage) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setSlippageTolerance(slippage);
        settingsRepo.save(s);
    }

    public void updateOrderType(Long chatId, String orderType) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setOrderType(orderType);
        settingsRepo.save(s);
    }

    public void updateNotificationsEnabled(Long chatId, Boolean enabled) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setNotificationsEnabled(enabled);
        settingsRepo.save(s);
    }

    public void updateModelVersion(Long chatId, String version) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setModelVersion(version);
        settingsRepo.save(s);
    }

    public void updateCachedCandlesLimit(Long chatId, Integer limit) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setCachedCandlesLimit(limit);
        settingsRepo.save(s);
    }

    public void updateStrategy(Long chatId, String strategyCode) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setStrategy(StrategyType.valueOf(strategyCode));
        settingsRepo.save(s);
    }

    public void updateMaxPositions(Long chatId, Integer maxPositions) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setMaxPositions(maxPositions);
        settingsRepo.save(s);
    }

    public void setRunning(Long chatId, boolean isRunning) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setIsRunning(isRunning);
        settingsRepo.save(s);
    }

    public void updateTpSl(Long chatId, String tpSlJson) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTpSlConfig(tpSlJson);
        settingsRepo.save(s);
    }

    // === Сброс к дефолтам ===

    public void resetTimeframeDefaults(Long chatId) { updateTimeframe(chatId, defaults.getDefaultTimeframe()); }
    public void resetTopNDefaults(Long chatId)    { updateTopN(chatId, defaults.getDefaultTopN()); }
    public void resetRiskThresholdDefaults(Long chatId) { updateRiskThreshold(chatId, defaults.getDefaultRiskThreshold()); }
    public void resetLeverageDefaults(Long chatId) { updateLeverage(chatId, defaults.getDefaultLeverage()); }
    public void resetMaxDrawdownDefaults(Long chatId) { updateMaxDrawdown(chatId, defaults.getDefaultMaxDrawdown()); }
    public void resetTradeCooldownDefaults(Long chatId) { updateTradeCooldown(chatId, defaults.getDefaultTradeCooldown()); }
    public void resetSlippageToleranceDefaults(Long chatId) { updateSlippageTolerance(chatId, defaults.getDefaultSlippageTolerance()); }
    public void resetOrderTypeDefaults(Long chatId) { updateOrderType(chatId, defaults.getDefaultOrderType()); }
    public void resetNotificationsEnabledDefaults(Long chatId) { updateNotificationsEnabled(chatId, defaults.isDefaultNotificationsEnabled()); }
    public void resetModelVersionDefaults(Long chatId) { updateModelVersion(chatId, defaults.getDefaultModelVersion()); }
    public void resetCachedCandlesLimitDefaults(Long chatId) { updateCachedCandlesLimit(chatId, defaults.getDefaultCachedCandlesLimit()); }
    public void resetSymbolsDefaults(Long chatId) { updateSymbols(chatId, defaults.getDefaultSymbols()); }
    public void resetStrategyDefaults(Long chatId) { updateStrategy(chatId, defaults.getDefaultStrategy()); }
    public void resetMaxPositionsDefaults(Long chatId) { updateMaxPositions(chatId, defaults.getDefaultMaxPositions()); }

    // === Утилитарные методы ===

    public List<String> suggestPairs(Long chatId) {
        int topN = Optional.ofNullable(getOrCreate(chatId).getTopN()).orElse(defaults.getDefaultTopN());
        return pairRepo.findByUserChatId(chatId).stream()
                .sorted((a, b) -> Double.compare(b.getTakeProfitPct(), a.getTakeProfitPct()))
                .limit(topN)
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.toList());
    }

    public List<Long> findAllChatIds() {
        return settingsRepo.findAll().stream()
                .map(AiTradingSettings::getChatId)
                .collect(Collectors.toList());
    }

    public AiTradingSettings getSettingsOrThrow(Long chatId) {
        return settingsRepo.findByUserChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("AiTradingSettings not found for chatId=" + chatId));
    }

    public List<AiTradingSettings> findAllRunning() {
        return settingsRepo.findByIsRunningTrue();
    }

    public List<AiTradingSettings> getAllActiveTrading() {
        return settingsRepo.findByIsRunningTrue();
    }
    /**
     * Сохраняет переданные настройки в базу.
     */
    public AiTradingSettings save(AiTradingSettings settings) {
        return settingsRepo.save(settings);
    }

}
