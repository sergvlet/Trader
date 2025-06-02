// src/main/java/com/chicu/trader/strategy/rsiema/RsiEmaStrategy.java
package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RsiEmaStrategy implements TradeStrategy {

    private final RsiEmaStrategySettingsService configService;

    /**
     * Логика RSI+EMA:
     * BUY  — RSI < rsiBuyThreshold && emaShort > emaLong
     * SELL — RSI > rsiSellThreshold && emaShort < emaLong
     * иначе HOLD.
     */
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

    /** Тип этой стратегии — используется для регистрации в StrategyRegistry */
    @Override
    public StrategyType getType() {
        return StrategyType.RSI_EMA;
    }
}
