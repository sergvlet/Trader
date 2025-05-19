// src/main/java/com/chicu/trader/bot/config/AiTradingDefaults.java
package com.chicu.trader.bot.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AiTradingDefaults {
    // любые дефолты, которые вы хотите
    private final String networkMode  = "real";      // или "test"
    private final double defaultTp    = 0.03;
    private final double defaultSl    = 0.01;
    private final boolean defaultReinvest = false;
    private final int    defaultTopN  = 5;
    // … другие дефолты
}
