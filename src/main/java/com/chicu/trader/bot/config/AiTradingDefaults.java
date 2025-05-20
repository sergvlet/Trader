package com.chicu.trader.bot.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AiTradingDefaults {

    /** Сетевая конфигурация: "real" или "test" */
    private final String networkMode = "real";

    /** Take-Profit по умолчанию (3%) */
    private final double defaultTp = 0.03;

    /** Stop-Loss по умолчанию (1%) */
    private final double defaultSl = 0.01;

    /** Режим реинвестирования по умолчанию */
    private final boolean defaultReinvest = false;

    /** Top N: сколько пар брать (по умолчанию 5) */
    private final int defaultTopN = 5;

    /** Таймфрейм по умолчанию ("1h") */
    private final String defaultTimeframe = "1h";

    /** Пары по умолчанию (пусто = все) */
    private final String defaultSymbols = "";

    /** Процент баланса на сделку по умолчанию (1%) */
    private final double defaultRiskThreshold = 1.0;

    /** Максимальная просадка по умолчанию (5%) */
    private final double defaultMaxDrawdown = 5.0;

    /** Комиссия биржи по умолчанию (0.1%) */
    private final double defaultCommission = 0.1;

    /** Максимальное число одновременных позиций по умолчанию */
    private final int defaultMaxPositions = 3;

    /** Задержка между сделками по умолчанию (1 минута) */
    private final int defaultTradeCooldown = 1;

    /** Допустимое проскальзывание по умолчанию (0.1%) */
    private final double defaultSlippageTolerance = 0.1;

    /** Тип ордера по умолчанию ("MARKET") */
    private final String defaultOrderType = "MARKET";

    /** Уведомления о сделках включены по умолчанию */
    private final boolean defaultNotificationsEnabled = true;

    /** Версия ML-модели по умолчанию */
    private final String defaultModelVersion = "v1";

    /** Плечо по умолчанию (1х) */
    private final int defaultLeverage = 1;

    /** Количество свечей для кэширования по умолчанию */
    private final int defaultCachedCandlesLimit = 500;
}
