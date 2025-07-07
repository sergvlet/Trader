package com.chicu.trader.strategy.scalping;

import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;
import com.chicu.trader.strategy.scalping.service.ScalpingStrategySettingsService;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScalpingStrategy implements TradeStrategy {

    private final ScalpingStrategySettingsService settingsService;

    @Override
    public StrategyType getType() {
        return StrategyType.SCALPING;
    }

    @Override
    public SignalType evaluate(List<Candle> candles, StrategySettings strategySettings) {
        ScalpingStrategySettings cfg = (ScalpingStrategySettings) strategySettings;
        int window = cfg.getWindowSize();

        if (candles.size() < window) {
            log.debug("Not enough data for scalping: have {}, need {}", candles.size(), window);
            return SignalType.HOLD;
        }

        // Берём последние window свечей
        List<Candle> windowCandles = candles.subList(candles.size() - window, candles.size());
        double first = windowCandles.get(0).getClose();
        double last  = windowCandles.get(window - 1).getClose();

        // 1) Считаем EMA
        double emaFirst = calculateEma(windowCandles, cfg.getEmaSmoothing());
        double emaLast  = calculateEma(windowCandles.subList(1, window), cfg.getEmaSmoothing());
        double emaChange = (emaLast - emaFirst) / emaFirst * 100;

        // 2) Считаем средний объём
        OptionalDouble avgVolOpt = windowCandles.stream()
                .mapToDouble(Candle::getVolume)
                .average();
        double avgVol = avgVolOpt.orElse(0.0);
        double volThreshold = avgVol * cfg.getVolumeThresholdMultiplier();
        double currVol = windowCandles.get(window - 1).getVolume();

        // 3) Динамический порог (рост √window * базовый порог)
        double dynamicThreshold = cfg.getPriceChangeThreshold() * Math.sqrt(window);

        log.debug("Scalping[{}]: priceΔ={:.2f}% (dynThr={:.2f}%), emaΔ={:.2f}%, vol={:.0f}/{:.0f}",
                new Object[]{cfg.getWindowSize(), (last - first) / first * 100, dynamicThreshold, emaChange, currVol, volThreshold});

        // Сигналы
        if (emaChange >= dynamicThreshold && currVol >= volThreshold) {
            return SignalType.BUY;
        } else if (emaChange <= -dynamicThreshold && currVol >= volThreshold) {
            return SignalType.SELL;
        }
        return SignalType.HOLD;
    }

    /** Простая EMA: α = smoothing/(1+window), window = размер списка */
    private double calculateEma(List<Candle> candles, int smoothing) {
        double alpha = smoothing / (1.0 + candles.size());
        double ema = candles.get(0).getClose();
        for (int i = 1; i < candles.size(); i++) {
            ema = alpha * candles.get(i).getClose() + (1 - alpha) * ema;
        }
        return ema;
    }

    @Override
    public StrategySettings getSettings(Long chatId) {
        return settingsService.getOrCreate(chatId);
    }

    @Override
    public boolean isTrainable() {
        return false;
    }

    @Override
    public void train(Long chatId) {
        // обучение не требуется
    }
}
