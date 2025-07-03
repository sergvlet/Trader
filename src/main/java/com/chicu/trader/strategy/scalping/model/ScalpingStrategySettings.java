package com.chicu.trader.strategy.scalping.model;

import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "scalping_strategy_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ScalpingStrategySettings extends StrategySettings {

    @Column(name = "window_size", nullable = false)
    private Integer windowSize;

    @Column(name = "price_change_threshold", nullable = false)
    private Double priceChangeThreshold;

    @Column(name = "min_volume", nullable = false)
    private Double minVolume;

    @Column(name = "spread_threshold", nullable = false)
    private Double spreadThreshold;

    @Column(name = "take_profit_pct", nullable = false)
    private Double takeProfitPct;

    @Column(name = "stop_loss_pct", nullable = false)
    private Double stopLossPct;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "timeframe", nullable = false)
    private String timeframe;

    @Column(name = "cached_candles_limit", nullable = false)
    private Integer cachedCandlesLimit;

    @Version
    private Long version;

    @Override
    public StrategyType getType() {
        return StrategyType.SCALPING;
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

