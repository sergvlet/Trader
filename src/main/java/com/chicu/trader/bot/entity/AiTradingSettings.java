package com.chicu.trader.bot.entity;

import com.chicu.trader.strategy.StrategyType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_trading_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTradingSettings {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "chat_id")
    private User user;

    @Column(name = "network_mode", length = 32)
    private String networkMode;

    @Column(name = "tp_sl_config", length = 128)
    private String tpSlConfig;

    @Builder.Default
    @Column(name = "reinvest_enabled", nullable = false)
    private Boolean reinvestEnabled = false;

    @Column(name = "symbols", length = 256)
    private String symbols;

    @Builder.Default
    @Column(name = "top_n", nullable = false)
    private Integer topN = 5;

    @Column(name = "risk_threshold")
    private Double riskThreshold;

    @Column(name = "max_drawdown")
    private Double maxDrawdown;

    @Column(name = "timeframe", length = 16)
    private String timeframe;

    @Column(name = "commission")
    private Double commission;

    @Column(name = "max_positions")
    private Integer maxPositions;

    @Column(name = "trade_cooldown")
    private Integer tradeCooldown;

    @Column(name = "slippage_tolerance")
    private Double slippageTolerance;

    @Column(name = "order_type", length = 16)
    private String orderType;

    @Builder.Default
    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true;

    @Column(name = "model_version", length = 64)
    private String modelVersion;

    @Column(name = "leverage")
    private Integer leverage;

    @Builder.Default
    @Column(name = "cached_candles_limit")
    private Integer cachedCandlesLimit = 500;

    @Column(name = "ml_model_path")
    private String mlModelPath;

    @Column(name = "ml_input_name")
    private String mlInputName;

    @Column(name = "ml_threshold")
    private Double mlThreshold;

    @Column(name = "ml_accuracy")
    private Double mlAccuracy;

    @Column(name = "ml_recall")
    private Double mlRecall;

    @Column(name = "ml_precision")
    private Double mlPrecision;

    @Column(name = "ml_auc")
    private Double mlAuc;

    @Column(name = "ml_trained_at")
    private Long mlTrainedAt;

    /** Новый ключевой флаг работы бота */
    @Builder.Default
    @Column(name = "is_running", nullable = false)
    private Boolean isRunning = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", length = 64, nullable = false)
    private StrategyType strategy = StrategyType.DEFAULT;

    @Version
    private Long version;
}
