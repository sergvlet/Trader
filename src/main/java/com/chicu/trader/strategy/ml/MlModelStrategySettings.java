package com.chicu.trader.strategy.ml;

import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "ml_model_strategy_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class MlModelStrategySettings extends StrategySettings {

    /** Путь к файлу модели, например: "models/ml_model_rf.pkl" */
    @Column(name = "model_path", length = 255)
    private String modelPath;

    /** Список признаков, например: "close,volume,rsi,ema20" */
    @Column(name = "feature_list", length = 512)
    private String featureList;

    /** Порог вероятности (0.0–1.0), при котором даётся сигнал BUY/SELL */
    @Column(name = "threshold")
    private Double threshold;

    /** Время последнего обучения модели */
    @Column(name = "last_trained_at")
    private Instant lastTrainedAt;

    // — опционально: гиперпараметры
    @Column(name = "n_estimators")
    private Integer nEstimators;

    @Column(name = "max_depth")
    private Integer maxDepth;

    @Column(name = "learning_rate")
    private Double learningRate;

    @Version
    private Long version;

    @Override
    public StrategyType getType() {
        return StrategyType.ML_MODEL;
    }
}
