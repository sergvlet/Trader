package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderResponse {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("orderId")
    private Long orderId;

    @JsonProperty("transactTime")
    private Long transactTime;

    @JsonProperty("price")
    private String price;

    @JsonProperty("origQty")
    private String origQty;

    @JsonProperty("executedQty")
    private String executedQty;

    @JsonProperty("status")
    private String status;
}
