package com.chicu.trader.strategy.rsiema.model;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;

import com.chicu.trader.trading.util.DoubleListConverter;
import com.chicu.trader.trading.util.IntegerListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "rsi_ema_strategy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class RsiEmaStrategySettings extends StrategySettings {

    @Id
    private Long chatId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", referencedColumnName = "chat_id", insertable = false, updatable = false)
    private AiTradingSettings aiTradingSettings;

    /** ✅ Основные параметры для торговли */
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

    @Column(name = "take_profit_pct", nullable = false)
    private Double takeProfitPct;

    @Column(name = "stop_loss_pct", nullable = false)
    private Double stopLossPct;

    @Column(name = "take_profit_window")
    private Integer takeProfitWindow;

    @Column(name = "cached_candles_limit")
    private Integer cachedCandlesLimit;

    @Column(name = "timeframe")
    private String timeframe;

    @Column(name = "symbol")
    private String symbol;

    /** ✅ Параметры для перебора в ML и переобучении — в одной таблице */
    @Convert(converter = IntegerListConverter.class)
    @Column(name = "rsi_periods")
    private List<Integer> rsiPeriods;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "ema_shorts")
    private List<Integer> emaShorts;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "ema_longs")
    private List<Integer> emaLongs;

    @Convert(converter = DoubleListConverter.class)
    @Column(name = "rsi_buy_thresholds")
    private List<Double> rsiBuyThresholds;

    @Convert(converter = DoubleListConverter.class)
    @Column(name = "rsi_sell_thresholds")
    private List<Double> rsiSellThresholds;

    @Version
    private Long version;

    @Override
    public StrategyType getType() {
        return StrategyType.RSI_EMA;
    }
}
