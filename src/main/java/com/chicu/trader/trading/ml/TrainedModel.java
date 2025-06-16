package com.chicu.trader.trading.ml;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class TrainedModel {
    private final Object internalModel; // ваш объект модели (он может быть из XGBoost, DJL и т.п.)
    private final double accuracy;
    private final double auc;
    private final double precision;
    private final double recall;
}
