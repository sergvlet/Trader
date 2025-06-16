package com.chicu.trader.trading.ml;
public interface ModelExporter {
    void export(TrainedModel model, String path) throws MlTrainingException;
}
