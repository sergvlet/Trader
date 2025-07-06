package com.chicu.trader.trading.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "backtest_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSettings {

    /** ID Telegram-пользователя */
    @Id
    private Long chatId;

    /** Дата начала теста (включительно) */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Дата окончания теста (включительно) */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Комиссия override для бэктеста, в процентах (например, 0.1) */
    @Column(name = "commission_pct", nullable = false)
    private Double commissionPct;

    /** Проскальзывание (slippage) в процентах (например, 0.1) */
    @Column(name = "slippage_pct", nullable = false)
    private Double slippagePct;

    /** Таймфрейм, например: "1m", "15m", "1h" */
    @Column(name = "timeframe", nullable = false)
    private String timeframe;

    /** Лимит кэшируемых свечей для стратегии */
    @Column(name = "cached_candles_limit", nullable = false)
    private Integer cachedCandlesLimit;

    /** Плечо, используемое в тесте */
    @Column(name = "leverage", nullable = false)
    private Integer leverage;
}
