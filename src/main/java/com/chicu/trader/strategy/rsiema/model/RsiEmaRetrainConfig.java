package com.chicu.trader.strategy.rsiema.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Хранит параметры перебора для переобучения RSI+EMA стратегии.
 * Все значения задаются через Telegram или вручную в БД.
 */
@Entity
@Table(name = "rsi_ema_retrain_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RsiEmaRetrainConfig {

    @Id
    private Long chatId;

    /** Возможные значения RSI-периодов */
    @ElementCollection
    @CollectionTable(name = "rsi_ema_rsi_periods", joinColumns = @JoinColumn(name = "chat_id"))
    @Column(name = "rsi_period")
    private List<Integer> rsiPeriods;

    /** Значения короткой EMA */
    @ElementCollection
    @CollectionTable(name = "rsi_ema_ema_short", joinColumns = @JoinColumn(name = "chat_id"))
    @Column(name = "ema_short")
    private List<Integer> emaShorts;

    /** Значения длинной EMA */
    @ElementCollection
    @CollectionTable(name = "rsi_ema_ema_long", joinColumns = @JoinColumn(name = "chat_id"))
    @Column(name = "ema_long")
    private List<Integer> emaLongs;

    /** RSI-порог для покупки */
    @ElementCollection
    @CollectionTable(name = "rsi_ema_buy_thresholds", joinColumns = @JoinColumn(name = "chat_id"))
    @Column(name = "buy_threshold")
    private List<Double> rsiBuyThresholds;

    /** RSI-порог для продажи */
    @ElementCollection
    @CollectionTable(name = "rsi_ema_sell_thresholds", joinColumns = @JoinColumn(name = "chat_id"))
    @Column(name = "sell_threshold")
    private List<Double> rsiSellThresholds;

    /** TakeProfit — % прибыли, при котором считается успешная сделка */
    @Column(name = "take_profit_pct", nullable = false)
    private Double takeProfitPct;

    /** StopLoss — % убытка, при котором сделка считается неудачной */
    @Column(name = "stop_loss_pct", nullable = false)
    private Double stopLossPct;

    /** Кол-во свечей вперёд для оценки результата (TP/SL окно) */
    @Column(name = "tp_window", nullable = false)
    private Integer takeProfitWindow;

    @Version
    private Long version;
}
