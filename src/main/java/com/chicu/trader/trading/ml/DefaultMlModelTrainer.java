package com.chicu.trader.trading.ml;

import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.trading.entity.Candle;
import com.chicu.trader.trading.ml.dataset.Dataset;
import com.chicu.trader.trading.ml.dataset.DatasetBuilder;
import com.chicu.trader.trading.ml.features.FeatureExtractor;
import com.chicu.trader.trading.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final CandleRepository candleRepository;
    private final AiTradingSettingsRepository settingsRepository;
    private final ModelTrainerInternal trainer;
    private final FeatureExtractor extractor;

    @Override
    public MlTrainingMetrics trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        // 1) Загрузка настроек
        var settings = settingsRepository.findById(chatId)
                .orElseThrow(() -> new MlTrainingException("Настройки не найдены для chatId=" + chatId));

        // 2) Первая пара и таймфрейм
        String symbol = settings.getSymbols().split(",")[0].trim();
        String timeframe = settings.getTimeframe();

        // 3) Загрузка свечей
        List<Candle> candles = candleRepository.findBySymbolAndTimeframeOrderByTimestampAsc(symbol, timeframe);
        if (candles.size() < 21) {
            throw new MlTrainingException("Недостаточно данных: найдено " + candles.size() + " свечей");
        }

        // 4) Построение датасета (окно 20)
        DatasetBuilder builder = new DatasetBuilder(extractor, 20);
        Dataset dataset = builder.build(candles);  // здесь используется com.chicu.trader.trading.ml.dataset.Dataset

        // 5) Обучение внутренним тренером
        TrainedModel model = trainer.train(dataset);

        // 6) Сохранение модели в ONNX
        model.saveToOnnx(modelPath);

        // 7) Сбор метрик
        return MlTrainingMetrics.builder()
                .accuracy(model.getAccuracy())
                .auc(model.getAuc())
                .precision(model.getPrecision())
                .recall(model.getRecall())
                .trainingTimeMillis(model.getTrainingTimeMillis())
                .build();
    }
}
