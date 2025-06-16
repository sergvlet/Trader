// src/main/java/com/chicu/trader/trading/context/StrategyContext.java
package com.chicu.trader.trading.context;

import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class StrategyContext {

    /** ID пользователя (chatId) */
    @Getter
    private final Long chatId;
    /** Текущая закрывшаяся свеча */
    @Getter
    private final Candle candle;
    /** Остальные поля контекста (по необходимости) */
    private final List<String>     symbols;
    private final CandleService    candleService;
    private final IndicatorService indicatorService;
    private final MlSignalFilter   mlFilter;

    public StrategyContext(Long chatId,
                           Candle candle,
                           List<String> symbols,
                           CandleService candleService,
                           IndicatorService indicatorService,
                           MlSignalFilter mlFilter) {
        this.chatId           = chatId;
        this.candle           = candle;
        this.symbols          = symbols;
        this.candleService    = candleService;
        this.indicatorService = indicatorService;
        this.mlFilter         = mlFilter;
    }

    /** Символ текущей свечи, например "ETHUSDT" */
    public String getSymbol() {
        return candle.getSymbol();
    }

    /** Цена закрытия текущей свечи, обёрнутая в BigDecimal */
    public BigDecimal getPrice() {
        return BigDecimal.valueOf(candle.getClose());
    }

    /** Stop Loss = 0.99 * close */
    public BigDecimal getSlPrice() {
        return BigDecimal.valueOf(candle.getClose())
                .multiply(BigDecimal.valueOf(0.99));
    }

    /** Take Profit = 1.01 * close */
    public BigDecimal getTpPrice() {
        return BigDecimal.valueOf(candle.getClose())
                .multiply(BigDecimal.valueOf(1.01));
    }

    /**
     * Количество для ордера.
     * Здесь просто объём свечи, но вы можете подставить свою логику.
     */
    public BigDecimal getOrderQuantity() {
        return BigDecimal.valueOf(candle.getVolume());
    }

    /** ML-фильтр (заглушка – всегда true) */
    public boolean passesMlFilter()      { return true; }
    /** Фильтр по объёму (заглушка – всегда true) */
    public boolean passesVolume()        { return true; }
    /** Фильтр по мультивременным рамкам (заглушка – всегда true) */
    public boolean passesMultiTimeframe(){ return true; }
    /** Фильтр RSI + BB (заглушка – всегда true) */
    public boolean passesRsiBb()         { return true; }

    /**
     * Сигнал выхода (если нужно закрыть позицию автоматически).
     * По умолчанию – пустой.
     */
    public Optional<ExitLog> getExitLog() {
        return Optional.empty();
    }

    /**
     * Класс для сигнала выхода: chatId, symbol, exitPrice (BigDecimal).
     */
    @Getter
    @RequiredArgsConstructor
    public static class ExitLog {
        private final Long       chatId;
        private final String     symbol;
        private final BigDecimal exitPrice;
    }
}
