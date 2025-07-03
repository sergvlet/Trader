package com.chicu.trader.bot.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /** Стратегия по умолчанию */
    private final String defaultStrategy = "RSI_EMA";

    // === RSI/EMA параметры по умолчанию ===

    private final int defaultRsiPeriod = 14;
    private final double defaultRsiBuyThreshold = 30.0;
    private final double defaultRsiSellThreshold = 70.0;
    private final int defaultEmaShort = 9;
    private final int defaultEmaLong = 21;

    /** Параметры для переобучения RSI/EMA */
    private final List<Integer> defaultRsiPeriods = List.of(10, 14, 20, 26);
    private final List<Integer> defaultEmaShorts = List.of(5, 9, 12);
    private final List<Integer> defaultEmaLongs = List.of(20, 21, 26, 30);
    private final List<Double> defaultRsiBuyThresholds = List.of(25.0, 30.0, 35.0);
    private final List<Double> defaultRsiSellThresholds = List.of(65.0, 70.0, 75.0);
    private final double defaultTakeProfitPct = 2.0;
    private final double defaultStopLossPct = 1.0;
    private final int defaultTakeProfitWindow = 12;
}
