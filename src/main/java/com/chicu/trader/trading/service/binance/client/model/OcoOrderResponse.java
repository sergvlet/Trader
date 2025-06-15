package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OcoOrderResponse {

    @JsonProperty("orderListId")
    private Long orderListId;

    @JsonProperty("contingencyType")
    private String contingencyType;

    @JsonProperty("listStatusType")
    private String listStatusType;

    @JsonProperty("listOrderStatus")
    private String listOrderStatus;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("orders")
    private List<Order> orders;

    @Data
    public static class Order {
        @JsonProperty("orderId")
        private Long orderId;

        @JsonProperty("symbol")
        private String symbol;
    }
}
