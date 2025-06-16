package com.chicu.trader.trading.ml.dataset;

import lombok.Getter;

@Getter
public class Dataset {
    private final double[][] X;
    private final int[] y;

    public Dataset(double[][] X, int[] y) {
        this.X = X;
        this.y = y;
    }
}
