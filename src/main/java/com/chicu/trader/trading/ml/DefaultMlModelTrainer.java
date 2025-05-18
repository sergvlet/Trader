// src/main/java/com/chicu/trader/trading/ml/DefaultMlModelTrainer.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.CandleService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Простая заглушка: переобучает модель и экспортирует ONNX.
 */
@Service
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final CandleService candleService;
    private final ProfitablePairRepository pairRepo;

    @Override
    public void trainAndExport(String onnxPath) {
        // TODO: сюда вашу реальную логику:
        // 1) собрать исторические свечи по всем активным ProfitablePair
        // 2) извлечь из них фичи
        // 3) обучить XGBClassifier
        // 4) экспортировать модель в ONNX по пути onnxPath
    }
}
