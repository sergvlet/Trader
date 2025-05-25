package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;

import com.chicu.trader.model.SignalType;
import com.chicu.trader.trading.model.Candle;

import java.util.List;

/**
 * TradeStrategy — базовый интерфейс всех торговых стратегий.
 * Используется в AI-режиме для генерации торговых сигналов.
 *
 * Реализация должна быть Stateless и потокобезопасной.
 */
public interface TradeStrategy {

    /**
     * Получить торговый сигнал на основе истории свечей и настроек пользователя.
     *
     * @param candles  список свечей (возможно, разных таймфреймов)
     * @param settings настройки конкретного пользователя
     * @return BUY / SELL / HOLD
     */
    SignalType evaluate(List<Candle> candles, AiTradingSettings settings);

    /**
     * Уникальный код стратегии (например, RSI_EMA, ML_MODEL, SCALPING).
     */
    String code();

    /**
     * Отображаемое имя (для меню выбора).
     */
    String label();
}
