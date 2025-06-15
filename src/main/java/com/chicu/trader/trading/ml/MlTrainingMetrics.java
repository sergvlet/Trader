package com.chicu.trader.trading.ml;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MlTrainingMetrics {
    private double accuracy;
    private double auc;
    private double precision;
    private double recall;
}
