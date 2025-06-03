package com.chicu.trader.ml.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ml_signal_filter_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlSignalFilterConfig {

    @Id
    private Long chatId;

    @Column(nullable = false)
    private double minChangeThreshold;

    @Version
    private Long version;
}
