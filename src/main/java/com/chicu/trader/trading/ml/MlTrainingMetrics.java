// src/main/java/com/chicu/trader/trading/ml/MlTrainingMetrics.java
package com.chicu.trader.trading.ml;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Метрики обучения: accuracy, auc и пр.
 */
@Getter
@RequiredArgsConstructor
public class MlTrainingMetrics {
    private final double accuracy;
    private final double auc;
    private final String modelPath;
    private final long durationMillis;
    private final String notes;
}
