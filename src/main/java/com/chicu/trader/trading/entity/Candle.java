package com.chicu.trader.trading.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "candles",
       indexes = {
         @Index(name = "idx_candle_symbol_timeframe_ts", columnList = "symbol, timeframe, timestamp")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Символ торговой пары, например "ETHUSDT" */
    @Column(nullable = false, length = 20)
    private String symbol;

    /** Таймфрейм, например "1h", "15m" */
    @Column(nullable = false, length = 10)
    private String timeframe;

    /** Время открытия свечи (unix-timestamp UTC) */
    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal close;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal volume;
}
