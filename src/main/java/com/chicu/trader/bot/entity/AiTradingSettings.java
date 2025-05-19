// src/main/java/com/chicu/trader/bot/entity/AiTradingSettings.java
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

    /** TP/SL в процентах (например "0.03,0.01") или в виде JSON */
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

    /** Порог риска (например максимально допустимая просадка в процентах) */
    @Column(name = "risk_threshold")
    private Double riskThreshold;

    /** Макс. просадка (drawdown) в процентах */
    @Column(name = "max_drawdown")
    private Double maxDrawdown;

    /** Таймфрейм (например "HOURLY", "DAILY") */
    @Column(name = "timeframe", length = 16)
    private String timeframe;

    /** Биржевая комиссия в промилле или процентах */
    @Column(name = "commission")
    private Double commission;

    /** Версия для оптимистичной блокировки */
    @Version
    private Long version;
}
