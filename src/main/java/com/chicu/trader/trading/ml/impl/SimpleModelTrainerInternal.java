package com.chicu.trader.trading.ml.impl;

import com.chicu.trader.trading.ml.ModelTrainerInternal;
import com.chicu.trader.trading.ml.TrainedModel;
import com.chicu.trader.trading.ml.dataset.Dataset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimpleModelTrainerInternal implements ModelTrainerInternal {

    @Override
    public TrainedModel train(Dataset dataset) {
        long start = System.currentTimeMillis();

        // TODO: здесь ваша реальная логика обучения на dataset.getX() и dataset.getY()
        double dummyAccuracy  = 0.5;
        double dummyAuc       = 0.5;
        double dummyPrecision = 0.5;
        double dummyRecall    = 0.5;

        long elapsed = System.currentTimeMillis() - start;
        log.info("SimpleModelTrainerInternal: обучили модель за {} мс", elapsed);

        return TrainedModel.builder()
                .accuracy(dummyAccuracy)
                .auc(dummyAuc)
                .precision(dummyPrecision)
                .recall(dummyRecall)
                .trainingTimeMillis(elapsed)
                .build();
    }
}
