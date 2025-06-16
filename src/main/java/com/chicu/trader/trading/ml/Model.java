// src/main/java/com/chicu/trader/trading/ml/Model.java
package com.chicu.trader.trading.ml;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Model {
    private final double[] weights;
    private final double bias;
}
