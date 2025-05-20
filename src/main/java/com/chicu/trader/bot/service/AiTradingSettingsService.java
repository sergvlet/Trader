package com.chicu.trader.bot.service;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.bot.repository.UserRepository;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.DailyOptimizer;
import com.chicu.trader.trading.ml.MlModelTrainer;
import com.chicu.trader.trading.ml.MlTrainingMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiTradingSettingsService {

    private final AiTradingSettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final AiTradingDefaults defaults;
    private final ProfitablePairRepository pairRepo;
    private final MlModelTrainer modelTrainer;
    private final AiTradingService aiTradingService;
    private final DailyOptimizer optimizer;

    public AiTradingSettingsService(AiTradingSettingsRepository settingsRepo,
                                    UserRepository userRepo,
                                    AiTradingDefaults defaults,
                                    ObjectMapper objectMapper,
                                    ProfitablePairRepository pairRepo,
                                    MlModelTrainer modelTrainer,
                                    AiTradingService aiTradingService,
                                    @Lazy DailyOptimizer optimizer) {
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.defaults = defaults;
        this.pairRepo = pairRepo;
        this.modelTrainer = modelTrainer;
        this.aiTradingService = aiTradingService;
        this.optimizer = optimizer;
    }

    @Transactional
    public AiTradingSettings getOrCreate(Long chatId) {
        return settingsRepo.findById(chatId)
                .orElseGet(() -> {
                    User user = userRepo.findById(chatId)
                            .orElseThrow(() -> new IllegalStateException("User not found: " + chatId));
                    ObjectNode tpSl = JsonNodeFactory.instance.objectNode()
                            .put("tp", defaults.getDefaultTp())
                            .put("sl", defaults.getDefaultSl());
                    AiTradingSettings s = AiTradingSettings.builder()
                            .user(user)
                            .networkMode(defaults.getNetworkMode())
                            .tpSlConfig(tpSl.toString())
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
                            .mlModelPath("models/%d/ml_signal_filter.onnx")
                            .mlInputName("input")
                            .mlThreshold(0.5)
                            .build();
                    log.info("Созданы настройки AI для chatId={}", chatId);
                    return settingsRepo.save(s);
                });
    }

    public void updateTpSl(Long chatId, String tpSlJson) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTpSlConfig(tpSlJson);
        settingsRepo.save(s);
    }

    public void resetTpSlDefaults(Long chatId) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode()
                .put("tp", defaults.getDefaultTp())
                .put("sl", defaults.getDefaultSl());
        updateTpSl(chatId, obj.toString());
    }

    public void updateTimeframe(Long chatId, String timeframe) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTimeframe(timeframe);
        settingsRepo.save(s);
        trainAndApplyAsync(chatId);
    }

    public void resetTimeframeDefaults(Long chatId) {
        updateTimeframe(chatId, defaults.getDefaultTimeframe());
    }

    public void updateTopN(Long chatId, Integer topN) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTopN(topN);
        settingsRepo.save(s);
        trainAndApplyAsync(chatId);
    }

    public void resetTopNDefaults(Long chatId) {
        updateTopN(chatId, defaults.getDefaultTopN());
    }

    public void updateSymbols(Long chatId, String symbolsCsv) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setSymbols(symbolsCsv);
        settingsRepo.save(s);
        trainAndApplyAsync(chatId);
    }

    public void resetSymbolsDefaults(Long chatId) {
        updateSymbols(chatId, defaults.getDefaultSymbols());
    }

    public void updateRiskThreshold(Long chatId, Double riskPercent) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setRiskThreshold(riskPercent);
        settingsRepo.save(s);
    }

    public void resetRiskDefaults(Long chatId) {
        updateRiskThreshold(chatId, defaults.getDefaultRiskThreshold());
    }

    public void updateMaxDrawdown(Long chatId, Double drawdownPercent) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setMaxDrawdown(drawdownPercent);
        settingsRepo.save(s);
    }

    public void resetMaxDrawdownDefaults(Long chatId) {
        updateMaxDrawdown(chatId, defaults.getDefaultMaxDrawdown());
    }

    public void updateLeverage(Long chatId, Integer leverage) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setLeverage(leverage);
        settingsRepo.save(s);
    }

    public void resetLeverageDefaults(Long chatId) {
        updateLeverage(chatId, defaults.getDefaultLeverage());
    }

    public void updateMaxPositions(Long chatId, Integer maxPositions) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setMaxPositions(maxPositions);
        settingsRepo.save(s);
    }

    public void resetMaxPositionsDefaults(Long chatId) {
        updateMaxPositions(chatId, defaults.getDefaultMaxPositions());
    }

    public void updateTradeCooldown(Long chatId, Integer minutes) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTradeCooldown(minutes);
        settingsRepo.save(s);
    }

    public void resetTradeCooldownDefaults(Long chatId) {
        updateTradeCooldown(chatId, defaults.getDefaultTradeCooldown());
    }

    public void updateSlippageTolerance(Long chatId, Double tolerance) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setSlippageTolerance(tolerance);
        settingsRepo.save(s);
    }

    public void resetSlippageToleranceDefaults(Long chatId) {
        updateSlippageTolerance(chatId, defaults.getDefaultSlippageTolerance());
    }

    public void updateOrderType(Long chatId, String type) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setOrderType(type);
        settingsRepo.save(s);
    }

    public void resetOrderTypeDefaults(Long chatId) {
        updateOrderType(chatId, defaults.getDefaultOrderType());
    }

    public void updateNotificationsEnabled(Long chatId, Boolean enabled) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setNotificationsEnabled(enabled);
        settingsRepo.save(s);
    }

    public void resetNotificationsEnabledDefaults(Long chatId) {
        updateNotificationsEnabled(chatId, defaults.isDefaultNotificationsEnabled());
    }

    public void updateModelVersion(Long chatId, String version) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setModelVersion(version);
        settingsRepo.save(s);
    }

    public void resetModelVersionDefaults(Long chatId) {
        updateModelVersion(chatId, defaults.getDefaultModelVersion());
    }

    public List<String> suggestPairs(Long chatId) {
        AiTradingSettings s = getOrCreate(chatId);
        int topN = Optional.ofNullable(s.getTopN()).orElse(defaults.getDefaultTopN());
        return pairRepo.findByUserChatId(chatId).stream()
                .filter(ProfitablePair::isActive)
                .sorted(Comparator.comparing(ProfitablePair::getTakeProfitPct).reversed())
                .limit(topN)
                .map(ProfitablePair::getSymbol)
                .collect(Collectors.toList());
    }

    @Async("mlExecutor")
    public CompletableFuture<Void> trainAndApplyAsync(Long chatId) {
        log.info("🔄 Запуск обучения и оптимизации для chatId={}", chatId);

        String path = String.format("models/%d/ml_signal_filter.onnx", chatId);
        MlTrainingMetrics metrics = modelTrainer.trainAndExport(chatId, path);
        var res = optimizer.optimizeAllForChat(chatId);

        AiTradingSettings s = getOrCreate(chatId);
        s.setTpSlConfig(res.toJson());
        s.setTopN(res.getTopN());
        s.setSymbols(String.join(",", res.getSymbols()));
        s.setTimeframe(res.getTimeframe());
        s.setRiskThreshold(res.getRiskThreshold());
        s.setMaxDrawdown(res.getMaxDrawdown());
        s.setLeverage(res.getLeverage());
        s.setMaxPositions(res.getMaxPositions());
        s.setTradeCooldown(res.getTradeCooldown());
        s.setSlippageTolerance(res.getSlippageTolerance());
        s.setOrderType(res.getOrderType());
        s.setNotificationsEnabled(res.getNotificationsEnabled());
        s.setModelVersion(res.getModelVersion());

        s.setMlAccuracy(metrics.getAccuracy());
        s.setMlPrecision(metrics.getPrecision());
        s.setMlRecall(metrics.getRecall());
        s.setMlAuc(metrics.getAuc());
        s.setMlTrainedAt(System.currentTimeMillis());

        settingsRepo.save(s);

        aiTradingService.enableTrading(chatId);
        log.info("✅ AI-торговля включена для chatId={}, метрики: acc=%.4f, pr=%.4f, rec=%.4f, auc=%.4f",
                chatId,
                metrics.getAccuracy(),
                metrics.getPrecision(),
                metrics.getRecall(),
                metrics.getAuc());

        return CompletableFuture.completedFuture(null);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Warsaw")
    public void dailyBacktestAll() {
        findAllChatIds().forEach(this::trainAndApplyAsync);
    }

    public List<Long> findAllChatIds() {
        return settingsRepo.findAll().stream()
                .map(AiTradingSettings::getChatId)
                .collect(Collectors.toList());
    }

    public AiTradingSettings save(AiTradingSettings settings) {
        return settingsRepo.save(settings);
    }

    public void updateCachedCandlesLimit(Long chatId, Integer limit) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setCachedCandlesLimit(limit);
        settingsRepo.save(s);
    }

    public void resetCachedCandlesLimitDefaults(Long chatId) {
        updateCachedCandlesLimit(chatId, defaults.getDefaultCachedCandlesLimit());
    }
}
