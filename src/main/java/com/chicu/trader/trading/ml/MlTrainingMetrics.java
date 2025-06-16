package com.chicu.trader.trading.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Простая структура метрик обучения.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MlTrainingMetrics {

    /**
     * Точность (accuracy) модели.
     */
    private double accuracy;

    /**
     * Площадь под ROC‐кривой (AUC).
     */
    private double auc;

    /**
     * Метрика precision.
     */
    private double precision;

    /**
     * Метрика recall.
     */
    private double recall;

    /**
     * Время обучения в миллисекундах.
     */
    private long trainingTimeMillis;
}
