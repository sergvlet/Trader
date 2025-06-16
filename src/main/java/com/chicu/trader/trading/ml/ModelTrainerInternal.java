package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.ml.dataset.Dataset;

public interface ModelTrainerInternal {
    TrainedModel train(Dataset dataset) throws MlTrainingException;
}
