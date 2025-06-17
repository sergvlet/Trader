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

    @Column(name = "exit_time")
    private Instant exitTime;

    @Builder.Default
    @Column(name = "exit_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal exitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "pnl", nullable = false, precision = 18, scale = 8)
    private BigDecimal pnl = BigDecimal.ZERO;

    /**
     * Поле-флаг закрытости сделки.
     * На уровне БД колонка называется is_closed,
     * но в Java-сущности свойство — closed.
     */
    @Builder.Default
    @Column(name = "is_closed", nullable = false)
    private Boolean closed = false;

    @Column(name = "take_profit_price", precision = 18, scale = 8)
    private BigDecimal takeProfitPrice;

    @Column(name = "stop_loss_price", precision = 18, scale = 8)
    private BigDecimal stopLossPrice;

    @Column(name = "client_order_id", nullable = true, unique = true, length = 64)
    private String clientOrderId;
}
