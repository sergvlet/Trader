package com.chicu.trader.strategy.fibonacciGridS.model;

import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fibonacci_grid_strategy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FibonacciGridStrategySettings extends StrategySettings {

    @Column(nullable = false)
    private Integer gridLevels;           // Количество уровней сетки (например, 5)

    @Column(nullable = false)
    private Double distancePct;           // Расстояние между уровнями в процентах (например, 0.5%)

    @Column(nullable = false)
    private Double baseAmount;            // Базовая сумма для входа (например, 100 USDT)

    @Column(nullable = false)
    private Double takeProfitPct;         // Take Profit в %

    @Column(nullable = false)
    private Double stopLossPct;          // Stop Loss в %

    @Column(nullable = false)
    private String timeframe;             // "1m", "15m" и т.п.

    @Column(nullable = false)
    private Integer cachedCandlesLimit;   // Кол-во свечей для анализа (например, 100)

    @Column(nullable = false)
    private String symbol;                // Торговая пара, например "BTCUSDT"

    @Version
    @Column(name = "version")
    private Long version;                 // Оптимистическая блокировка

    @Override
    public StrategyType getType() {
        return StrategyType.FIBONACCI_GRID;
    }

    @Override
    public String getTimeframe() {
        return timeframe;
    }

    @Override
    public Integer getCachedCandlesLimit() {
        return cachedCandlesLimit;
    }
}
