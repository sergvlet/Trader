package com.chicu.trader.trading.ml.impl;

import com.chicu.trader.trading.ml.MlTrainingException;
import com.chicu.trader.trading.ml.ModelTrainerInternal;
import com.chicu.trader.trading.ml.TrainedModel;
import com.chicu.trader.trading.ml.dataset.Dataset;
import ml.dmlc.xgboost4j.LabeledPoint;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class XgboostModelTrainerInternal implements ModelTrainerInternal {

    @Override
    public TrainedModel train(Dataset ds) throws MlTrainingException {
        try {
            long start = System.currentTimeMillis();

            double[][] x = ds.getX();
            int[]      y = ds.getY();
            int        n = x.length;

            // Формируем LabeledPoint (dense формат)
            List<LabeledPoint> points = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                float   label    = (float) y[i];
                float[] features = toFloatArray(x[i]);
                points.add(new LabeledPoint(label, features.length, null, features));
            }

            DMatrix trainMat = new DMatrix(points.iterator(), "nan");

            Map<String, Object> params = Map.of(
                    "eta",         0.1f,
                    "max_depth",   6,
                    "objective",   "binary:logistic",
                    "eval_metric", "auc"
            );

            // Исправленный вызов обучения (пустая Map вместо null!)
            Booster booster = XGBoost.train(trainMat, params, 100, Map.of(), null, null);

            float[][] preds = booster.predict(trainMat);
            double correct = 0;
            for (int i = 0; i < preds.length; i++) {
                int p = preds[i][0] > 0.5 ? 1 : 0;
                if (p == y[i]) correct++;
            }
            double accuracy = correct / n;

            byte[] modelBytes = booster.toByteArray();
            long elapsed = System.currentTimeMillis() - start;

            return TrainedModel.builder()
                    .onnxBytes(modelBytes)
                    .accuracy(accuracy)
                    .auc(accuracy)
                    .precision(accuracy)
                    .recall(accuracy)
                    .trainingTimeMillis(elapsed)
                    .build();

        } catch (Exception e) {
            throw new MlTrainingException("Ошибка XGBoost: " + e.getMessage(), e);
        }
    }


    private static float[] toFloatArray(double[] arr) {
        float[] f = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            f[i] = (float) arr[i];
        }
        return f;
    }
}
