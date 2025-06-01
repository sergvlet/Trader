package com.chicu.trader.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Хранит все TradeStrategy-биты, доступные в контексте Spring.
 * Маппит тип StrategyType → соответствующий бин.
 */
@Component
@RequiredArgsConstructor
public class StrategyRegistry {

    private final Map<String, TradeStrategy> strategiesByName;

    /**
     * Spring автоматически соберёт в Map все бины, реализующие TradeStrategy,
     * где ключ в Map – это beanName (по умолчанию «rsiEmaStrategy» или «mlModelStrategy» и т. д.).
     * Мы просто преобразуем из beanName → TradeStrategy в StrategyType → TradeStrategy.
     */
    public TradeStrategy getByType(StrategyType type) {
        // Перебираем все TradeStrategy-бены, ищем тот, чей getType() совпадает с type
        return strategiesByName.values().stream()
                .filter(s -> s.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Strategy not found for type: " + type));
    }

    /**
     * Можно вернуть сразу Map<StrategyType, TradeStrategy>, если нужно.
     */
    public Map<StrategyType, TradeStrategy> getMap() {
        return strategiesByName.values().stream()
                .collect(Collectors.toMap(TradeStrategy::getType, s -> s));
    }
}
