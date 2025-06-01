package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings; // сущность с пользовательскими настройками
import com.chicu.trader.trading.model.Candle;          // простой POJO для «свечи»

import java.util.List;

/**
 * Общий интерфейс любой торговой стратегии.
 * <p>
 * - evaluate(...) принимаeт список свечей и пользовательские настройки AI-торговли (AiTradingSettings).
 * - Возвращает один из трёх сигналов: BUY, SELL или HOLD.
 * <p>
 * Например: если объём последней свечи резко вырос — вернуть BUY, иначе HOLD.
 * Или: ML-модель на основе массива цен выдаёт BUY/SELL/HOLD.
 */
public interface TradeStrategy {

    /** Описаны возможные «торговые сигналы» */
    enum SignalType {
        BUY,
        SELL,
        HOLD
    }

    /**
     * Оценивает текущую ситуацию и возвращает сигнал.
     * 
     * @param candles  Список последних N свечей (самая свежая — последний элемент списка).
     * @param settings Объект с настройками пользователя (в том числе стратегия, TP/SL, параметры ML и т. д.).
     * @return BUY/SELL/HOLD
     */
    SignalType evaluate(List<Candle> candles, AiTradingSettings settings);

    /**
     * Возвращает тип этой стратегии (для того, чтобы фасад мог выбрать нужную реализацию).
     */
    StrategyType getType();
}
