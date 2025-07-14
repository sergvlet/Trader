package com.chicu.trader.strategy.fibonacciGridS.service.impl;

import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;
import com.chicu.trader.strategy.fibonacciGridS.repository.FibonacciGridStrategySettingsRepo;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FibonacciGridStrategySettingsServiceImpl implements FibonacciGridStrategySettingsService {

    private final FibonacciGridStrategySettingsRepo repo;
    private final AiTradingSettingsService aiSettingsService;

    @Override
    @Transactional
    public FibonacciGridStrategySettings getOrCreate(Long chatId) {
        return repo.findById(chatId).orElseGet(() -> {
            var ai = aiSettingsService.getOrCreate(chatId);

            FibonacciGridStrategySettings s = FibonacciGridStrategySettings.builder()
                    .chatId(chatId)
                    .aiTradingSettings(ai)
                    .symbol("BTCUSDT")
                    .gridLevels(5)
                    .distancePct(0.5)
                    .baseAmount(100.0)
                    .takeProfitPct(1.5)
                    .stopLossPct(1.0)
                    .timeframe("15m")
                    .cachedCandlesLimit(100)
                    .build();

            return repo.save(s);
        });
    }
    @Override
    public void save(FibonacciGridStrategySettings settings) {
        repo.saveAndFlush(settings);
    }
}
