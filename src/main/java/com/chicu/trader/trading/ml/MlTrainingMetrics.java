package com.chicu.trader.trading.ml;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MlTrainingMetrics {
    private double accuracy;
    private double precision;
    private double recall;
    private double auc;
}
