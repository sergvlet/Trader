package com.chicu.trader.trading.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    @Id
    private String id;

    private String symbol;
    private long openTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private long closeTime;
    private String timeframe;
    private long timestamp;
}
