package com.chicu.trader.trading.service.binance.client.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SymbolInfo {
    private String symbol;
    private BigDecimal lotSizeStep;
    private BigDecimal priceTickSize;

    public SymbolInfo(String symbol, BigDecimal lotSizeStep, BigDecimal priceTickSize) {
        this.symbol = symbol;
        this.lotSizeStep = lotSizeStep;
        this.priceTickSize = priceTickSize;
    }
}
