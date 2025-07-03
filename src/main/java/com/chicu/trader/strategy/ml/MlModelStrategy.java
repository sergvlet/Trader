package com.chicu.trader.strategy.ml;

import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.inference.PythonInferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MlModelStrategy implements TradeStrategy {

    private final MlModelStrategySettingsService settingsService;
    private final PythonInferenceService inferenceService;

    @Override
    public SignalType evaluate(List<Candle> candles, StrategySettings settings) {
        Long chatId = settings.getChatId();  // Получаем chatId из базового класса
        MlModelStrategySettings cfg = settingsService.getOrCreate(chatId);

        log.debug("ML_MODEL: chatId={}, model={}, features={}, threshold={}",
                chatId, cfg.getModelPath(), cfg.getFeatureList(), cfg.getThreshold());

        double[] features = extractFeatures(candles, cfg.getFeatureList());
        double probability = inferenceService.predict(cfg.getModelPath(), features);

        log.info("ML_MODEL: chatId={} → predict={} (threshold={})", chatId, probability, cfg.getThreshold());

        if (probability >= cfg.getThreshold()) {
            return SignalType.BUY;
        } else if (probability <= (1.0 - cfg.getThreshold())) {
            return SignalType.SELL;
        }
        return SignalType.HOLD;
    }

    @Override
    public StrategyType getType() {
        return StrategyType.ML_MODEL;
    }

    @Override
    public StrategySettings getSettings(Long chatId) {
        return settingsService.getOrCreate(chatId);
    }

    @Override
    public boolean isTrainable() {
        return true;
    }

    @Override
    public void train(Long chatId) {
        // TODO: добавить обучение модели, если применимо
        log.info("ML_MODEL: обучение не реализовано (chatId={})", chatId);
    }

    private double[] extractFeatures(List<Candle> candles, String featureList) {
        if (candles == null || candles.isEmpty()) return new double[0];

        Candle last = candles.get(candles.size() - 1);
        return Arrays.stream(featureList.split(","))
                .map(String::trim)
                .mapToDouble(f -> switch (f.toLowerCase()) {
                    case "close" -> last.getClose();
                    case "open" -> last.getOpen();
                    case "high" -> last.getHigh();
                    case "low" -> last.getLow();
                    case "volume" -> last.getVolume();
                    default -> {
                        log.warn("ML_MODEL: неизвестный признак '{}'", f);
                        yield 0.0;
                    }
                }).toArray();
    }
}
