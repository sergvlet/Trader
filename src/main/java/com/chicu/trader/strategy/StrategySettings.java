package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Абстрактный базовый класс для хранения настроек конкретных стратегий.
 * <p>
 * У каждой конкретной стратегии будет своя таблица с уникальными полями,
 * наследующаяся от этого класса.
 */
@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class StrategySettings {

    @Id
    protected Long chatId;

    /**
     * Связь с основной таблицей настроек AiTradingSettings.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "chat_id")
    protected AiTradingSettings aiTradingSettings;

    /**
     * Тип текущей стратегии (обязателен для всех реализаций).
     */
    public abstract StrategyType getType();

    /**
     * Таймфрейм, например: "1m", "1h", "4h"
     */
    public abstract String getTimeframe();

    /**
     * Количество свечей для загрузки (истории) при анализе
     */
    public abstract Integer getCachedCandlesLimit();
}
