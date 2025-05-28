// src/main/java/com/chicu/trader/strategy/rsiema/RsiEmaStrategySettings.java
package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rsi_ema_strategy_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RsiEmaStrategySettings {

    @Id
    private Long chatId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "chat_id")
    private AiTradingSettings aiSettings;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String timeframe;

    @Column(nullable = false)
    private Integer cachedCandlesLimit;

    @Column(name = "ema_short", nullable = false)
    private Integer emaShort;

    @Column(name = "ema_long", nullable = false)
    private Integer emaLong;

    @Column(name = "rsi_period", nullable = false)
    private Integer rsiPeriod;

    @Column(name = "rsi_buy_threshold", nullable = false)
    private Double rsiBuyThreshold;

    @Column(name = "rsi_sell_threshold", nullable = false)
    private Double rsiSellThreshold;

    @Version
    private Long version;
}
