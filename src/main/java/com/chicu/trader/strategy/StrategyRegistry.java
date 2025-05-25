package com.chicu.trader.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Хранилище всех доступных стратегий.
 */
@Component
@RequiredArgsConstructor
public class StrategyRegistry {

    private final List<TradeStrategy> allStrategies;

    public TradeStrategy findByCode(String code) {
        return allStrategies.stream()
                .filter(s -> s.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseGet(() -> allStrategies.stream()
                        .filter(s -> s.code().equalsIgnoreCase(StrategyType.DEFAULT.name()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Default strategy not found")));
    }

    public List<TradeStrategy> getAll() {
        return allStrategies;
    }

    public Map<String, String> getCodeToLabelMap() {
        return allStrategies.stream()
                .collect(Collectors.toMap(TradeStrategy::code, TradeStrategy::label));
    }
}
