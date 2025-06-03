package com.chicu.trader.strategy.retrain;

import com.chicu.trader.bot.entity.AiTradingSettings;

/**
 * Контракт для стратегий, поддерживающих переобучение (Retraining).
 * Каждая стратегия, реализующая этот интерфейс, должна уметь:
 * — запускать переобучение (например, обновление параметров по истории);
 * — сохранять полученные результаты в свою таблицу настроек.
 */
public interface StrategyRetrainer {

    /**
     * Запустить переобучение стратегии на основе истории.
     * Метод может использовать API Binance, статистику сделок, и т.д.
     * @param settings текущие настройки пользователя
     * @return true — если переобучение прошло успешно
     */
    boolean retrain(AiTradingSettings settings);
}
