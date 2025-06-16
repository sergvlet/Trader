// src/main/java/com/chicu/trader/trading/service/binance/client/model/SymbolInfo.java
package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolInfo {
    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("filters")
    private List<SymbolFilter> filters;
}
