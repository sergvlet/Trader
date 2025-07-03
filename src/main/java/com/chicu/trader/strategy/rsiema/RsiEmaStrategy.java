package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.ml.MlTrainingService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaStrategySettingsService;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RsiEmaStrategy implements TradeStrategy {

    private final RsiEmaStrategySettingsService settingsService;
    private final MlTrainingService trainingService;

    @Override
    public SignalType evaluate(List<Candle> candles, StrategySettings settings) {
        Long chatId = settings.getChatId();
        log.debug("RsiEmaStrategy: начинаем evaluate для chatId={} с {} свечей", chatId, candles.size());

        if (candles.size() < 50) {
            log.warn("RsiEmaStrategy: недостаточно данных для анализа (требуется минимум 50)");
            return SignalType.HOLD;
        }

        RsiEmaStrategySettings cfg = (RsiEmaStrategySettings) settings;

        List<Double> closes = candles.stream().map(Candle::getClose).collect(Collectors.toList());
        double rsi = RsiCalculator.latest(closes, cfg.getRsiPeriod());
        double emaShort = EmaCalculator.latest(closes, cfg.getEmaShort());
        double emaLong = EmaCalculator.latest(closes, cfg.getEmaLong());

        log.debug("rsi={}, emaShort={}, emaLong={}", rsi, emaShort, emaLong);

        if (rsi < cfg.getRsiBuyThreshold() && emaShort > emaLong) {
            log.info("BUY сигнал: rsi={} < {}, emaShort={} > {}", rsi, cfg.getRsiBuyThreshold(), emaShort, emaLong);
            return SignalType.BUY;
        }

        if (rsi > cfg.getRsiSellThreshold() && emaShort < emaLong) {
            log.info("SELL сигнал: rsi={} > {}, emaShort={} < {}", rsi, cfg.getRsiSellThreshold(), emaShort, emaLong);
            return SignalType.SELL;
        }

        return SignalType.HOLD;
    }

    @Override
    public StrategyType getType() {
        return StrategyType.RSI_EMA;
    }

    @Override
    public StrategySettings getSettings(Long chatId) {
        return settingsService.getOrCreate(chatId);
    }

    @Override
    public void train(Long chatId) {
        log.info("🧠 Запуск обучения RSI/EMA для chatId={}", chatId);
        boolean result = trainingService.runTraining();
        if (result) {
            log.info("✅ Обучение завершено успешно");
        } else {
            log.warn("❌ Обучение завершилось с ошибкой");
        }
    }

    @Override
    public boolean isTrainable() {
        return true;
    }
}
