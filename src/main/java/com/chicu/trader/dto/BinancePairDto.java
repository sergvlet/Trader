package com.chicu.trader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinancePairDto {
    private String symbol;       // Пример: BTCUSDT
    private double price;        // Текущая цена
    private double priceChange;  // Изменение за 24ч в %
}
