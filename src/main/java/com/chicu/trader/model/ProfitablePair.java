// src/main/java/com/chicu/trader/model/ProfitablePair.java
package com.chicu.trader.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profitable_pairs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitablePair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_chat_id", nullable = false)
    private Long userChatId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "take_profit_pct", nullable = false)
    private double takeProfitPct;

    @Column(name = "stop_loss_pct", nullable = false)
    private double stopLossPct;

    @Column(nullable = false)
    private boolean active;
}
