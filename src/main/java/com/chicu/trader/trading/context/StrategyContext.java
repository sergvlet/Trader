// src/main/java/com/chicu/trader/trading/context/StrategyContext.java
package com.chicu.trader.trading.context;

import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.indicator.IndicatorService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Контекст исполнения стратегии:
 * - chatId        — ID пользователя
 * - candle        — текущая закрывшаяся свеча
 * - symbols       — список всех активных символов (ProfitablePair)
 * - candleService — сервис для работы со свечами (если нужно брать дополнительные данные)
 * - indicatorService — сервис для расчёта индикаторов (если нужны)
 * - mlFilter      — ML‐фильтр (если используется ML)
 *
 * Содержит заглушки для всех фильтров, а также метод getExitLog(), возвращающий
 * Optional<ExitLog> (по умолчанию — пустой, чтобы не закрывать сделки немедленно).
 */
public class StrategyContext {

    /**
     * -- GETTER --
     * ID пользователя, для которого мы торгуем
     */
    @Getter
    private final Long chatId;
    /**
     * -- GETTER --
     * Текущая свеча
     */
    @Getter
    private final Candle candle;
    private final List<String> symbols;
    private final CandleService candleService;
    private final IndicatorService indicatorService;
    private final MlSignalFilter mlFilter;

    public StrategyContext(Long chatId,
                           Candle candle,
                           List<String> symbols,
                           CandleService candleService,
                           IndicatorService indicatorService,
                           MlSignalFilter mlFilter) {
        this.chatId = chatId;
        this.candle = candle;
        this.symbols = symbols;
        this.candleService = candleService;
        this.indicatorService = indicatorService;
        this.mlFilter = mlFilter;
    }

    /** Символ (например "BTCUSDT") текущей свечи */
    public String getSymbol() {
        return candle.getSymbol();
    }

    /** Цена закрытия текущей свечи */
    public double getPrice() {
        return candle.getClose();
    }

    /**
     * Заглушка для расчёта Stop Loss.
     * В реальной реализации нужно вычислить из контекста (например, на основе ATR или %).
     * Здесь просто «цену*0.99»:
     */
    public double getSlPrice() {
        return candle.getClose() * 0.99;
    }

    /**
     * Заглушка для расчёта Take Profit.
     * В реальной реализации — на основе контекста (например, уровни сопротивления или %).
     * Здесь просто «цену*1.01»:
     */
    public double getTpPrice() {
        return candle.getClose() * 1.01;
    }

    /** ML-фильтр: если он есть и выдаёт false, то стратегию не запускаем. */
    public boolean passesMlFilter() {
        try {
            // Если модель есть, можно вызвать mlFilter.predict(...), но пока — всегда true
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Фильтр по объёму — заглушка (всегда true) */
    public boolean passesVolume() {
        return true;
    }

    /** Фильтр по мультивременным рамкам — заглушка (всегда true) */
    public boolean passesMultiTimeframe() {
        return true;
    }

    /** Фильтр RSI + Bollinger Bands — заглушка (всегда true) */
    public boolean passesRsiBb() {
        return true;
    }

    /**
     * Если нужно закрыть позицию (TP или SL сработали),
     * возвращаем Optional<ExitLog>. Здесь — всегда пустой Optional (не закрываем автоматически).
     *
     * В полноценной версии нужно проверить: если текущая цена >= TP или <= SL,
     * то вернуть new ExitLog(chatId, symbol, текущая цена).
     */
    public Optional<ExitLog> getExitLog() {
        return Optional.empty();
    }

    /**
     * Вложенный класс- контейнер для «сигнала выхода»
     */
    @Getter
    @RequiredArgsConstructor
    public static class ExitLog {
        private final Long chatId;
        private final String symbol;
        private final double exitPrice;
    }
}
