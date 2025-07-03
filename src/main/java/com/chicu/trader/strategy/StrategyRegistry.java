package com.chicu.trader.strategy;

import com.chicu.trader.strategy.ml.MlModelStrategy;
import com.chicu.trader.strategy.rsiema.RsiEmaStrategy;
import com.chicu.trader.strategy.scalping.ScalpingStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StrategyRegistry {

    private final MlModelStrategy mlModelStrategy;
    private final RsiEmaStrategy rsiEmaStrategy;
    private final ScalpingStrategy scalpingStrategy;

    private final Map<StrategyType, TradeStrategy> registry = new EnumMap<>(StrategyType.class);

    /**
     * Инициализация стратегий при старте Spring
     */
    @PostConstruct
    public void init() {
        registry.put(StrategyType.ML_MODEL, mlModelStrategy);
        registry.put(StrategyType.RSI_EMA, rsiEmaStrategy);
        registry.put(StrategyType.SCALPING, scalpingStrategy);
    }

    /**
     * Получить стратегию по типу
     */
    public TradeStrategy getStrategy(StrategyType type) {
        TradeStrategy strategy = registry.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy not registered: " + type);
        }
        return strategy;
    }

    /**
     * Получить настройки стратегии для указанного пользователя
     */
    public StrategySettings getSettings(StrategyType strategyType, Long chatId) {
        return getStrategy(strategyType).getSettings(chatId);
    }
}
