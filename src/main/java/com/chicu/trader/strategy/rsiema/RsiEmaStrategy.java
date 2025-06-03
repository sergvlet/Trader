package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.ml.MlTrainingService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaStrategySettingsService;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Стратегия RSI+EMA:
 * BUY  — RSI < rsiBuyThreshold && emaShort > emaLong
 * SELL — RSI > rsiSellThreshold && emaShort < emaLong
 * иначе HOLD.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RsiEmaStrategy implements TradeStrategy {

    private final RsiEmaStrategySettingsService configService;
    private final MlTrainingService trainingService;

    @Override
    public SignalType evaluate(List<Candle> candles, AiTradingSettings settings) {
        Long chatId = settings.getChatId();
        log.debug("RsiEmaStrategy: начинаем evaluate для chatId={} с {} свечей", chatId, candles.size());

        // Собираем цены закрытия
        List<Double> closes = candles.stream()
                .map(Candle::getClose)
                .collect(Collectors.toList());

        // Загружаем параметры стратегии для этого пользователя
        RsiEmaStrategySettings cfg = configService.getOrCreate(chatId);
        log.debug("RsiEmaStrategy: загружены настройки (rsiPeriod={}, rsiBuyThreshold={}, rsiSellThreshold={}, emaShort={}, emaLong={}) для chatId={}",
                cfg.getRsiPeriod(), cfg.getRsiBuyThreshold(), cfg.getRsiSellThreshold(),
                cfg.getEmaShort(), cfg.getEmaLong(), chatId);

        double rsi  = RsiCalculator.latest(closes, cfg.getRsiPeriod());
        double emaS = EmaCalculator.latest(closes, cfg.getEmaShort());
        double emaL = EmaCalculator.latest(closes, cfg.getEmaLong());
        log.debug("RsiEmaStrategy: вычислено rsi={}, emaShort={}, emaLong={} для chatId={}", rsi, emaS, emaL, chatId);

        if (rsi < cfg.getRsiBuyThreshold() && emaS > emaL) {
            log.info("RsiEmaStrategy: BUY сигнал для chatId={} (rsi={} < {}, emaShort={} > {})",
                    chatId, rsi, cfg.getRsiBuyThreshold(), emaS, emaL);
            return SignalType.BUY;
        }
        if (rsi > cfg.getRsiSellThreshold() && emaS < emaL) {
            log.info("RsiEmaStrategy: SELL сигнал для chatId={} (rsi={} > {}, emaShort={} < {})",
                    chatId, rsi, cfg.getRsiSellThreshold(), emaS, emaL);
            return SignalType.SELL;
        }

        log.debug("RsiEmaStrategy: HOLD для chatId={} (rsi={}, emaShort={}, emaLong={})", chatId, rsi, emaS, emaL);
        return SignalType.HOLD;
    }

    @Override
    public StrategyType getType() {
        return StrategyType.RSI_EMA;
    }

    /**
     * Метод для запуска обучения (если требуется для стратегии).
     */
    public void train(Long chatId) {
        log.info("RsiEmaStrategy: запуск обучения для chatId={}", chatId);
        boolean success = trainingService.runTraining(); // можно передавать chatId при необходимости
        if (success) {
            log.info("RsiEmaStrategy: обучение завершилось успешно для chatId={}", chatId);
        } else {
            log.warn("RsiEmaStrategy: обучение завершилось с ошибкой для chatId={}", chatId);
        }
    }

    /**
     * Используется в StrategyRegistry и при сканировании для определения, нужно ли обучение.
     */
    public boolean isTrainable() {
        return true;
    }
}
