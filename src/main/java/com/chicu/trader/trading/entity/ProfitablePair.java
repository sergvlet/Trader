package com.chicu.trader.trading.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profitable_pairs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_chat_id", "symbol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitablePair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_chat_id", nullable = false)
    private Long userChatId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "take_profit_pct", nullable = false)
    private Double takeProfitPct;

    @Column(name = "stop_loss_pct", nullable = false)
    private Double stopLossPct;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
