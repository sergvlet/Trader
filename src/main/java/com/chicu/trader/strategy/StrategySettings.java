package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Абстрактный базовый класс для хранения настроек конкретных стратегий.
 * <p>
 * У каждой конкретной стратегии будет своя таблица с уникальными полями,
 * наследующаяся от этого класса.
 */
@Getter
@Setter
@MappedSuperclass
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
     * Метод, возвращающий тип текущей стратегии.
     * Каждая реализация должна явно указать свой StrategyType.
     */
    public abstract StrategyType getType();
}
