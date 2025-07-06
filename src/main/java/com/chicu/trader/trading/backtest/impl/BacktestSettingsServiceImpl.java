package com.chicu.trader.trading.backtest.impl;

import com.chicu.trader.trading.backtest.repository.BacktestSettingsRepository;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import com.chicu.trader.trading.model.BacktestSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BacktestSettingsServiceImpl implements BacktestSettingsService {

    private static final double DEFAULT_COMMISSION_PCT   = 0.1;
    private static final double DEFAULT_SLIPPAGE_PCT     = 0.1;
    private static final String DEFAULT_TIMEFRAME        = "15m";
    private static final int    DEFAULT_CACHED_CANDLES   = 500;
    private static final int    DEFAULT_LEVERAGE         = 1;

    private final BacktestSettingsRepository repo;

    /**
     * Получает или создает настройки бэктеста по chatId.
     * По умолчанию задает последние 30 дней, комиссию 0.1%,
     * проскальзывание 0.1%, таймфрейм "15m", 500 свечей, плечо 1.
     */
    @Override
    @Transactional
    public BacktestSettings getOrCreate(Long chatId) {
        return repo.findById(chatId).orElseGet(() -> {
            BacktestSettings def = BacktestSettings.builder()
                    .chatId(chatId)
                    .startDate(LocalDate.now().minusDays(30))
                    .endDate(LocalDate.now())
                    .commissionPct(DEFAULT_COMMISSION_PCT)
                    .slippagePct(DEFAULT_SLIPPAGE_PCT)
                    .timeframe(DEFAULT_TIMEFRAME)
                    .cachedCandlesLimit(DEFAULT_CACHED_CANDLES)
                    .leverage(DEFAULT_LEVERAGE)
                    .build();
            return repo.save(def);
        });
    }

    /**
     * Сохраняет настройки бэктеста, подставляя дефолтное slippagePct, если оно не указано.
     */
    @Override
    @Transactional
    public void save(BacktestSettings settings) {
        if (settings.getSlippagePct() == null) {
            settings.setSlippagePct(DEFAULT_SLIPPAGE_PCT);
        }
        repo.save(settings);
    }

    /**
     * Обновляет период теста.
     */
    @Override
    @Transactional
    public void updatePeriod(Long chatId, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }
        BacktestSettings cfg = getOrCreate(chatId);
        cfg.setStartDate(start);
        cfg.setEndDate(end);
        repo.save(cfg);
    }

    /**
     * Обновляет комиссию.
     */
    @Override
    @Transactional
    public void updateCommission(Long chatId, Double commissionPct) {
        if (commissionPct < 0 || commissionPct > 100) {
            throw new IllegalArgumentException("Комиссия должна быть от 0 до 100%");
        }
        BacktestSettings cfg = getOrCreate(chatId);
        cfg.setCommissionPct(commissionPct);
        repo.save(cfg);
    }

    /**
     * Обновляет проскальзывание.
     */
    @Override
    @Transactional
    public void updateSlippage(Long chatId, Double slippagePct) {
        if (slippagePct < 0 || slippagePct > 100) {
            throw new IllegalArgumentException("Проскальзывание должно быть от 0 до 100%");
        }
        BacktestSettings cfg = getOrCreate(chatId);
        cfg.setSlippagePct(slippagePct);
        repo.save(cfg);
    }

    /**
     * Обновляет таймфрейм (например, "1m", "15m", "1h").
     */
    @Override
    @Transactional
    public void updateTimeframe(Long chatId, String timeframe) {
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("Таймфрейм не может быть пустым");
        }
        BacktestSettings cfg = getOrCreate(chatId);
        cfg.setTimeframe(timeframe);
        repo.save(cfg);
    }

    /**
     * Обновляет лимит свечей.
     */
    @Override
    @Transactional
    public void updateCachedCandlesLimit(Long chatId, int limit) {
        if (limit < 10 || limit > 5000) {
            throw new IllegalArgumentException("Допустимый диапазон: 10 — 5000");
        }
        BacktestSettings cfg = getOrCreate(chatId);
        cfg.setCachedCandlesLimit(limit);
        repo.save(cfg);
    }

    /**
     * Обновляет плечо.
     */
    @Override
    @Transactional
    public void updateLeverage(Long chatId, int leverage) {
        if (leverage < 1 || leverage > 125) {
            throw new IllegalArgumentException("Плечо должно быть от 1 до 125");
        }
        BacktestSettings cfg = getOrCreate(chatId);
        cfg.setLeverage(leverage);
        repo.save(cfg);
    }
}
