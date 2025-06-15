package com.chicu.trader.trading.service;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.binance.BinanceExchangeInfoService;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntryService {

    private final TradeLogRepository tradeLogRepository;
    private final OrderService orderService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final AccountService accountService;

    public void enterTrade(Long chatId, String symbol, double entryPrice, AiTradingSettings settings, ProfitablePair pair) {

        boolean hasOpen = tradeLogRepository.existsByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        if (hasOpen) {
            log.warn("Entry ▶ already open trade chatId={} symbol={}", chatId, symbol);
            return;
        }

        String quoteAsset = detectQuoteAsset(symbol);
        BigDecimal freeBalance = accountService.getFreeBalance(chatId, quoteAsset);
        if (freeBalance.compareTo(BigDecimal.ZERO) <= 0) return;

        double riskPct = settings.getRiskThreshold() != null ? settings.getRiskThreshold() : 0.0;
        BigDecimal amountToSpend = freeBalance.multiply(BigDecimal.valueOf(riskPct / 100.0));
        if (amountToSpend.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal slipFactor = BigDecimal.valueOf(1.0 - (settings.getSlippageTolerance() != null ? settings.getSlippageTolerance() : 0.0) / 100.0);
        BigDecimal spendable = amountToSpend.multiply(slipFactor);
        BigDecimal rawQty = spendable.divide(BigDecimal.valueOf(entryPrice), 8, RoundingMode.DOWN);
        BigDecimal stepSize = exchangeInfoService.getLotSizeStep(symbol);
        BigDecimal qtyBd = rawQty.divide(stepSize, 0, RoundingMode.DOWN).multiply(stepSize);
        if (qtyBd.compareTo(stepSize) < 0) return;

        orderService.placeMarketBuy(chatId, symbol, qtyBd);  // <- используем BigDecimal qty

        log.info("Entry ▶ Market BUY placed chatId={} symbol={} qty={}", chatId, symbol, qtyBd);

        BigDecimal tickSize = exchangeInfoService.getPriceTickSize(symbol);
        BigDecimal tp = BigDecimal.valueOf(entryPrice).multiply(BigDecimal.valueOf(1.0 + riskPct / 100.0))
                .divide(tickSize, 0, RoundingMode.DOWN).multiply(tickSize);

        BigDecimal sl = BigDecimal.valueOf(entryPrice).multiply(BigDecimal.valueOf(1.0 - riskPct / 100.0))
                .divide(tickSize, 0, RoundingMode.DOWN).multiply(tickSize);

        try {
            orderService.placeOcoSell(chatId, symbol, qtyBd, sl, tp);
        } catch (Exception e) {
            log.warn("Entry ▶ OCO order failed {}", e.getMessage());
        }

        tradeLogRepository.save(TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(entryPrice)
                .quantity(qtyBd.doubleValue())
                .takeProfitPrice(tp)
                .stopLossPrice(sl)
                .isClosed(false)
                .build());
    }

    private String detectQuoteAsset(String symbol) {
        if (symbol.endsWith("USDT")) return "USDT";
        if (symbol.endsWith("BUSD")) return "BUSD";
        if (symbol.endsWith("BTC")) return "BTC";
        if (symbol.endsWith("ETH")) return "ETH";
        return symbol.replaceFirst("^[A-Z]+", "");
    }
}
