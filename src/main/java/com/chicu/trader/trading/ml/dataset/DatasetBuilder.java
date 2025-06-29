package com.chicu.trader.trading.ml.dataset;


import com.chicu.trader.trading.ml.MlTrainingException;
import com.chicu.trader.trading.ml.features.FeatureExtractor;
import com.chicu.trader.trading.model.Candle;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class DatasetBuilder {

    private final FeatureExtractor extractor;
    private final int windowSize;

    /**
     * Построить Dataset из списка свечей.
     * @throws MlTrainingException если данных недостаточно
     */
    public Dataset build(List<Candle> allCandles) throws MlTrainingException {
        int n = allCandles.size();
        if (n <= windowSize) {
            throw new MlTrainingException("Недостаточно данных для формирования датасета");
        }

        List<double[]> X = new ArrayList<>();
        List<Integer> y = new ArrayList<>();

        // каждое окно — от [i-windowSize .. i), метка: 1 если close(i+1)>close(i)
        for (int i = windowSize; i < n - 1; i++) {
            List<Candle> window = allCandles.subList(i - windowSize, i);
            double[] feats = extractor.extract(window);
            X.add(feats);

            double curr = allCandles.get(i).getClose();
            double next = allCandles.get(i + 1).getClose();
            y.add(next > curr ? 1 : 0);
        }

        // конвертация в массивы
        double[][] xArr = X.toArray(new double[X.size()][]);
        int[] yArr    = y.stream().mapToInt(Integer::intValue).toArray();

        // возвращаем именно ml.dataset.Dataset
        return new Dataset(xArr, yArr);
    }
}
