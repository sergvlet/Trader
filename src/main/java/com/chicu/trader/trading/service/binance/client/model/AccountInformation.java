package com.chicu.trader.trading.service.binance.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountInformation {

    private Long updateTime;

    private List<Balance> balances;

    @Data
    public static class Balance {
        private String asset;

        @JsonProperty("free")
        private BigDecimal free;

        @JsonProperty("locked")
        private BigDecimal locked;
    }
}
