// src/main/java/com/chicu/trader/trading/service/binance/client/model/SymbolFilter.java
package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolFilter {
    @JsonProperty("filterType")
    private String filterType;

    /** Для PRICE_FILTER */
    @JsonProperty("tickSize")
    private String tickSize;

    /** Для LOT_SIZE */
    @JsonProperty("stepSize")
    private String stepSize;
}
