// src/main/java/com/chicu/trader/trading/util/QuantityAdjuster.java
package com.chicu.trader.trading.util;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolFilter;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Простейший корректировщик qty под лоты из ExchangeInfo.
 */
public class QuantityAdjuster {

    /**
     * Обрезает rawQty вниз до ближайшего лота из exchangeInfo для symbol.
     */
    public static BigDecimal adjustQuantity(String symbol, BigDecimal rawQty, ExchangeInfo info) {
        SymbolInfo si = info.getSymbols().stream()
            .filter(s->s.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Symbol not found: "+symbol));

        BigDecimal step = si.getFilters().stream()
            .filter(f->"LOT_SIZE".equals(f.getFilterType()))
            .map(SymbolFilter::getStepSize)
            .map(BigDecimal::new)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("LOT_SIZE missing"));

        int scale = step.stripTrailingZeros().scale();
        return rawQty.setScale(scale, RoundingMode.DOWN);
    }
}
