package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.entity.Candle;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Dataset {
    private double[][] features;
    private double[] labels;
    private List<Candle> candles;

    // Существующие конструкторы
    public Dataset() { }

    public Dataset(List<Candle> candles) {
        this.candles = candles;
        // здесь ваша логика подготовки features и labels из candles
    }

    // Добавляем этот конструктор
    public Dataset(double[][] features, double[] labels) {
        this.features = features;
        this.labels   = labels;
    }
}
