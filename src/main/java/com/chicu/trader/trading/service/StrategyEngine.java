package com.chicu.trader.trading.service;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.model.SignalType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyEngine {

    private final Map<String, TradeStrategy> strategies;

    public SignalType getSignal(Long chatId, String symbol, List<Candle> candles, AiTradingSettings settings) {
        String strategyCode = settings.getStrategy(); // ← должно быть поле strategy в AiTradingSettings
        if (strategyCode == null || strategyCode.isBlank()) {
            strategyCode = "RSI_EMA"; // стратегия по умолчанию
        }

        TradeStrategy strategy = strategies.get(strategyCode);
        if (strategy == null) {
            log.warn("❌ Не найдена стратегия: {}, используем HOLD", strategyCode);
            return SignalType.HOLD;
        }

        log.debug("📊 Используем стратегию {} для chatId={}, symbol={}", strategyCode, chatId, symbol);
        return strategy.evaluate(candles, settings);
    }
}
