// src/main/java/com/chicu/trader/trading/service/binance/client/model/SymbolFilter.java
package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Представляет один фильтр для торговой пары в ответе /exchangeInfo.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolFilter {
    @JsonProperty("filterType")
    private String filterType;

    // PRICE_FILTER
    @JsonProperty("minPrice")
    private String minPrice;
    @JsonProperty("maxPrice")
    private String maxPrice;
    @JsonProperty("tickSize")
    private String tickSize;

    // LOT_SIZE
    @JsonProperty("minQty")
    private String minQty;
    @JsonProperty("maxQty")
    private String maxQty;
    @JsonProperty("stepSize")
    private String stepSize;

    // MIN_NOTIONAL (если понадобится)
    @JsonProperty("minNotional")
    private String minNotional;

    /**
     * Удобные конвертеры в BigDecimal
     */
    public BigDecimal getMinPriceAsDecimal() {
        return minPrice == null ? null : new BigDecimal(minPrice);
    }

    public BigDecimal getMaxPriceAsDecimal() {
        return maxPrice == null ? null : new BigDecimal(maxPrice);
    }

    public BigDecimal getTickSizeAsDecimal() {
        return tickSize == null ? null : new BigDecimal(tickSize);
    }

    public BigDecimal getMinQtyAsDecimal() {
        return minQty == null ? null : new BigDecimal(minQty);
    }

    public BigDecimal getMaxQtyAsDecimal() {
        return maxQty == null ? null : new BigDecimal(maxQty);
    }

    public BigDecimal getStepSizeAsDecimal() {
        return stepSize == null ? null : new BigDecimal(stepSize);
    }

    public BigDecimal getMinNotionalAsDecimal() {
        return minNotional == null ? null : new BigDecimal(minNotional);
    }
}
