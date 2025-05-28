package com.chicu.trader.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Собирает все бины TradeStrategy и выдаёт нужную по типу.
 */
@Component
public class StrategyRegistry {

    private final Map<StrategyType, TradeStrategy> registry;

    /**
     * Spring инжектирует список всех TradeStrategy-бинов,
     * мы раскладываем их по ключу StrategyType.
     */
    @Autowired
    public StrategyRegistry(List<TradeStrategy> strategies) {
        Map<StrategyType, TradeStrategy> map = new EnumMap<>(StrategyType.class);
        for (TradeStrategy s : strategies) {
            map.put(s.getType(), s);
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    /**
     * Возвращает TradeStrategy для данного StrategyType.
     * Если не найдено — бросает исключение.
     */
    public TradeStrategy get(StrategyType type) {
        TradeStrategy strat = registry.get(type);
        if (strat == null) {
            throw new IllegalArgumentException("Unknown strategy type: " + type);
        }
        return strat;
    }

    /**
     * Все поддерживаемые StrategyType (для меню и валидации).
     */
    public Set<StrategyType> supported() {
        return registry.keySet();
    }
}
