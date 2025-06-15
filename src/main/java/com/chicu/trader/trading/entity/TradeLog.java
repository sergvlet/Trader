package com.chicu.trader.trading.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trade_log")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userChatId;

    private String symbol;

    private Instant entryTime;
    private Double entryPrice;
    private Double quantity;

    private Instant exitTime;
    private Double exitPrice;

    private Double pnl;

    private Boolean isClosed;

    private BigDecimal takeProfitPrice;
    private BigDecimal stopLossPrice;
}
