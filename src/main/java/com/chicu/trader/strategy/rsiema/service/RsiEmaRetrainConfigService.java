package com.chicu.trader.strategy.rsiema.service;

import com.chicu.trader.strategy.rsiema.model.RsiEmaRetrainConfig;
import com.chicu.trader.strategy.rsiema.repository.RsiEmaRetrainConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsiEmaRetrainConfigService {

    private final RsiEmaRetrainConfigRepository repo;

    @Transactional(readOnly = true)
    public RsiEmaRetrainConfig get(Long chatId) {
        return repo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("Не найдена конфигурация переобучения RSI/EMA для chatId=" + chatId));
    }

    @Transactional
    public RsiEmaRetrainConfig getOrCreateDefault(Long chatId) {
        return repo.findById(chatId).orElseGet(() -> createDefault(chatId));
    }

    @Transactional
    public RsiEmaRetrainConfig createDefault(Long chatId) {
        RsiEmaRetrainConfig config = RsiEmaRetrainConfig.builder()
                .chatId(chatId)
                .rsiPeriods(java.util.List.of(7, 10, 14))
                .emaShorts(java.util.List.of(5, 10, 15))
                .emaLongs(java.util.List.of(20, 30, 50))
                .rsiBuyThresholds(java.util.List.of(25.0, 30.0, 35.0))
                .rsiSellThresholds(java.util.List.of(65.0, 70.0, 75.0))
                .takeProfitPct(0.01)
                .stopLossPct(0.005)
                .takeProfitWindow(5)
                .build();
        return repo.save(config);
    }

    @Transactional
    public void save(RsiEmaRetrainConfig config) {
        repo.save(config);
    }
}
