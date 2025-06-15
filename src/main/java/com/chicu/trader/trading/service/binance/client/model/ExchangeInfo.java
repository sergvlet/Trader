package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ExchangeInfo {

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("serverTime")
    private Long serverTime;

    @JsonProperty("symbols")
    private List<SymbolInfo> symbols;
}
