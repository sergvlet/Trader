// src/main/java/com/chicu/trader/model/ProfitablePair.java
package com.chicu.trader.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profitable_pairs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitablePair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userChatId;
    private String symbol;
    private double takeProfitPct;
    private double stopLossPct;
    private boolean active;
}
