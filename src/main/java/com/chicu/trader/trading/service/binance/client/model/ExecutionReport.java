package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExecutionReport {
    @JsonProperty("e")   private String eventType;      // executionReport
    @JsonProperty("E")   private long   eventTime;
    @JsonProperty("s")   private String symbol;
    @JsonProperty("c")   private String clientOrderId;
    @JsonProperty("S")   private String side;           // BUY или SELL
    @JsonProperty("X")   private String status;         // NEW, PARTIALLY_FILLED, FILLED и т.п.
    @JsonProperty("p")   private String price;          // строка
    @JsonProperty("q")   private String quantity;
    @JsonProperty("f")   private String filledQuantity;
    // Добавьте другие поля при необходимости
}
