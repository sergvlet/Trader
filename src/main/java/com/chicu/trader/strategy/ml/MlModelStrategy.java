// src/main/java/com/chicu/trader/strategy/ml/MlModelStrategy.java
package com.chicu.trader.strategy.ml;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Стратегия на основе ML-инференса.
 * Счи�тывает цену закрытия свечей как фичи, передаёт в MlSignalFilter,
 * и на основе MarketSignal возвращает BUY / SELL / HOLD.
 */
@Component
@RequiredArgsConstructor
public class MlModelStrategy implements TradeStrategy {

    private final MlSignalFilter mlFilter;

    @Override
    public StrategyType getType() {
        return StrategyType.ML_MODEL;
    }

    @Override
    public SignalType evaluate(List<Candle> candles, AiTradingSettings settings) {
        // 1) Извлекаем цену закрытия каждой свечи и упаковываем в float[]
        int n = candles.size();
        float[] features = new float[n];
        for (int i = 0; i < n; i++) {
            features[i] = (float) candles.get(i).getClose();
        }

        // 2) Оборачиваем в MarketData
        MarketData data = new MarketData(features);

        // 3) Делаем инференс
        MarketSignal signal = mlFilter.predict(settings.getChatId(), data);

        // 4) Маппим результат в глобальный SignalType
        return switch (signal) {
            case BUY  -> SignalType.BUY;
            case SELL -> SignalType.SELL;
            default   -> SignalType.HOLD;
        };
    }
}
