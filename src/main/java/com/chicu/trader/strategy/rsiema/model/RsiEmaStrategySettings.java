package com.chicu.trader.strategy.rsiema.model;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rsi_ema_strategy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class RsiEmaStrategySettings extends StrategySettings {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", referencedColumnName = "chat_id", nullable = false)
    private AiTradingSettings aiTradingSettings;

    @Column(nullable = true)
    private String symbol;

    @Column(nullable = true)
    private String timeframe;

    @Column(name = "cached_candles_limit", nullable = true)
    private Integer cachedCandlesLimit;

    @Column(name = "ema_short", nullable = false)
    private Integer emaShort;

    @Column(name = "ema_long", nullable = false)
    private Integer emaLong;

    @Column(name = "rsi_period", nullable = false)
    private Integer rsiPeriod;

    @Column(name = "rsi_buy_threshold", nullable = true)
    private Double rsiBuyThreshold;

    @Column(name = "rsi_sell_threshold", nullable = true)
    private Double rsiSellThreshold;

    @Version
    private Long version;

    @Override
    public StrategyType getType() {
        return StrategyType.RSI_EMA;
    }
}
