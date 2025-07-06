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

    private final BacktestSettingsRepository repo;

    /**
     * Получает или создает настройки бэктеста по chatId.
     * По умолчанию задает последние 30 дней, комиссию 0.1%,
     * таймфрейм "15m", 500 свечей, плечо 1.
     */
    @Override
    @Transactional
    public BacktestSettings getOrCreate(Long chatId) {
        return repo.findById(chatId).orElseGet(() -> {
            BacktestSettings def = BacktestSettings.builder()
                    .chatId(chatId)
                    .startDate(LocalDate.now().minusDays(30))
                    .endDate(LocalDate.now())
                    .commissionPct(0.1)
                    .timeframe("15m")
                    .cachedCandlesLimit(500)
                    .leverage(1)
                    .build();
            return repo.save(def);
        });
    }

    /**
     * Сохраняет настройки бэктеста.
     */
    @Override
    @Transactional
    public void save(BacktestSettings settings) {
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
