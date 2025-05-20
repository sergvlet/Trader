package com.chicu.trader.bot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_trading_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTradingSettings {

    /** Используем chatId как PK и FK на таблицу users */
    @Id
    @Column(name = "chat_id")
    private Long chatId;

    /** Связь на объект User, то же chatId */
    @OneToOne
    @MapsId
    @JoinColumn(name = "chat_id")
    private User user;

    /** Сетевые настройки (например, URL API или режим тест/реал) */
    @Column(name = "network_mode", length = 32)
    private String networkMode;

    /** TP/SL в процентах (например JSON {"tp":0.03,"sl":0.01}) */
    @Column(name = "tp_sl_config", length = 128)
    private String tpSlConfig;

    /** Реинвестирование средств */
    @Column(name = "reinvest_enabled", nullable = false)
    private Boolean reinvestEnabled = false;

    /** Список пар через запятую */
    @Column(name = "symbols", length = 256)
    private String symbols; // например "BTCUSDT,ETHUSDT"

    /** Top N: сколько топ-пар брать */
    @Column(name = "top_n", nullable = false)
    private Integer topN = 5;

    /** Процент баланса на сделку (в %) */
    @Column(name = "risk_threshold")
    private Double riskThreshold;

    /** Макс. просадка (drawdown) в процентах */
    @Column(name = "max_drawdown")
    private Double maxDrawdown;

    /** Таймфрейм (например "1m", "4h", "1d") */
    @Column(name = "timeframe", length = 16)
    private String timeframe;

    /** Биржевая комиссия в процентах */
    @Column(name = "commission")
    private Double commission;

    // Новые поля для расширенных настроек AI-режима

    /** Максимальное число одновременных позиций */
    @Column(name = "max_positions")
    private Integer maxPositions;

    /** Задержка между сделками (в минутах) */
    @Column(name = "trade_cooldown")
    private Integer tradeCooldown;

    /** Допустимое проскальзывание (в %) */
    @Column(name = "slippage_tolerance")
    private Double slippageTolerance;

    /** Тип ордера ("MARKET", "LIMIT" и т.п.) */
    @Column(name = "order_type", length = 16)
    private String orderType;

    /** Включены ли уведомления о сделках */
    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled;

    /** Версия ML-модели для сигналов */
    @Column(name = "model_version", length = 64)
    private String modelVersion;

    /** Плечо (leverage) для маржинальной торговли */
    @Column(name = "leverage")
    private Integer leverage;

    @Column(name = "cached_candles_limit")
    private Integer cachedCandlesLimit = 500;

    /** Версия для оптимистичной блокировки */
    @Version
    private Long version;
}
