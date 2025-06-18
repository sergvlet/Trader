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

    @Column(name = "user_chat_id", nullable = false)
    private Long userChatId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "entry_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    // --- выходные поля теперь nullable ---

    @Column(name = "exit_time", nullable = true)
    private Instant exitTime;

    @Column(name = "exit_price", nullable = true, precision = 18, scale = 8)
    private BigDecimal exitPrice;

    @Column(name = "pnl", nullable = true, precision = 18, scale = 8)
    private BigDecimal pnl;

    /**
     * Поле-флаг закрытости сделки.
     */
    @Column(name = "is_closed", nullable = false)
    private Boolean closed = false;

    @Column(name = "take_profit_price", precision = 18, scale = 8, nullable = true)
    private BigDecimal takeProfitPrice;

    @Column(name = "stop_loss_price", precision = 18, scale = 8, nullable = true)
    private BigDecimal stopLossPrice;

    @Column(name = "entry_client_order_id", length = 64, unique = true, nullable = true)
    private String entryClientOrderId;

    @Column(name = "exit_client_order_id", length = 64, unique = true, nullable = true)
    private String exitClientOrderId;
}
