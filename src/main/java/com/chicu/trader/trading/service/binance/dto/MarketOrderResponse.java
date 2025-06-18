// MarketOrderResponse.java
package com.chicu.trader.trading.service.binance.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrderResponse {
    private String clientOrderId;
}
