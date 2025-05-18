// src/main/java/com/chicu/trader/model/TradeLog.java
package com.chicu.trader.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "trade_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userChatId;
    private String symbol;

    // вход
    private Instant entryTime;
    private double entryPrice;
    private double takeProfitPrice;
    private double stopLossPrice;
    private double quantity;        // <- добавлено

    // выход
    private boolean isClosed;
    private Instant exitTime;
    private double exitPrice;
    private double pnl;
}
