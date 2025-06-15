package com.chicu.trader.trading.risk;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultRiskManager implements RiskManager {

    private final AccountService accountService;

    @Override
    public double calculatePositionSize(Long chatId, String symbol, double entryPrice, AiTradingSettings settings) {

        String quoteAsset = detectQuoteAsset(symbol);
        BigDecimal freeBalance = accountService.getFreeBalance(chatId, quoteAsset);

        if (freeBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        double riskPct = Optional.ofNullable(settings.getRiskThreshold()).orElse(1.0);
        BigDecimal amountToRisk = freeBalance.multiply(BigDecimal.valueOf(riskPct / 100.0));

        BigDecimal qty = amountToRisk.divide(BigDecimal.valueOf(entryPrice), 8, RoundingMode.DOWN);

        return qty.doubleValue();
    }

    private String detectQuoteAsset(String symbol) {
        if (symbol.endsWith("USDT")) return "USDT";
        if (symbol.endsWith("BUSD")) return "BUSD";
        if (symbol.endsWith("BTC")) return "BTC";
        if (symbol.endsWith("ETH")) return "ETH";
        return symbol.replaceFirst("^[A-Z]+", "");
    }
}
