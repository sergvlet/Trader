package com.chicu.trader.trading.risk;

import com.chicu.trader.bot.entity.AiTradingSettings;

public interface RiskManager {
    double calculatePositionSize(Long chatId, String symbol, double price, AiTradingSettings settings);
}
