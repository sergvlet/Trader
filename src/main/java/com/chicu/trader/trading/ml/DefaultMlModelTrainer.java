package com.chicu.trader.trading.ml;

import com.chicu.trader.bot.repository.AiTradingSettingsRepository;


import com.chicu.trader.trading.ml.dataset.Dataset;
import com.chicu.trader.trading.ml.dataset.DatasetBuilder;
import com.chicu.trader.trading.ml.features.FeatureExtractor;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final CandleService candleService;
    private final AiTradingSettingsRepository settingsRepository;
    private final ModelTrainerInternal trainer;
    private final FeatureExtractor extractor;

    @Override
    public MlTrainingMetrics trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        // 1) Загрузка настроек
        var settings = settingsRepository.findById(chatId)
                .orElseThrow(() -> new MlTrainingException("Настройки не найдены для chatId=" + chatId));

        // 2) Первая пара и её таймфрейм
        String symbol = settings.getSymbols().split(",")[0].trim();
        Duration interval = parseTimeframe(settings.getTimeframe());
        int limit = settings.getCachedCandlesLimit();

        // 3) Загрузка свечей через CandleService
        List<Candle> candles = candleService.loadHistory(symbol, interval, limit);

        if (candles.size() < 21) {
            throw new MlTrainingException("Недостаточно данных: найдено " + candles.size() + " свечей");
        }

        // 4) Построение датасета с окном в 20 точек
        DatasetBuilder builder = new DatasetBuilder(extractor, 20);
        Dataset dataset = builder.build(candles);

        // 5) Обучение модели
        TrainedModel model = trainer.train(dataset);

        // 6) Сохранение в ONNX
        model.saveToOnnx(modelPath);

        // 7) Сбор метрик и возврат
        return MlTrainingMetrics.builder()
                .accuracy(model.getAccuracy())
                .auc(model.getAuc())
                .precision(model.getPrecision())
                .recall(model.getRecall())
                .trainingTimeMillis(model.getTrainingTimeMillis())
                .build();
    }

    private Duration parseTimeframe(String tf) {
        tf = tf.trim().toLowerCase();
        long value = Long.parseLong(tf.substring(0, tf.length() - 1));
        char unit = tf.charAt(tf.length() - 1);
        return switch (unit) {
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + tf);
        };
    }
}
