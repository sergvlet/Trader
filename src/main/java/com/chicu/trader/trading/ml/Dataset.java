// src/main/java/com/chicu/trader/trading/ml/Dataset.java
package com.chicu.trader.trading.ml;

public class Dataset {
    private final double[][] features;
    private final double[]   labels;

    public Dataset(double[][] features, double[] labels) {
        this.features = features;
        this.labels   = labels;
    }

    public double[][] getFeatures() { return features; }
    public double[]   getLabels()   { return labels; }
}
