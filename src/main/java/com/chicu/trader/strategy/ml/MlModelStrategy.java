package com.chicu.trader.strategy.ml;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.inference.PythonInferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ML-стратегия, которая использует обученную модель и вызывает Python-сервис
 * для оценки текущей ситуации.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MlModelStrategy implements TradeStrategy {

    private final MlModelStrategySettingsService settingsService;
    private final PythonInferenceService inferenceService;

    @Override
    public SignalType evaluate(List<Candle> candles, AiTradingSettings settings) {
        Long chatId = settings.getChatId();
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

    /**
     * Парсит список признаков и извлекает из свечей нужные значения.
     * Пример featureList: "close,volume"
     */
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
