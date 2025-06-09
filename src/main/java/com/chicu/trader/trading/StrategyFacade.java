package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.model.TradeLog;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.service.AccountService;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.OrderService;
import com.chicu.trader.trading.service.PriceService;
import com.chicu.trader.trading.service.binance.BinanceExchangeInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class StrategyFacade {

    private static final ConcurrentMap<String, ReentrantLock> tradeLocks = new ConcurrentHashMap<>();

    private final StrategyRegistry registry;
    private final AiTradingSettingsService settingsService;
    private final CandleService candleService;
    private final AccountService accountService;
    private final OrderService orderService;
    private final TradeLogRepository tradeLogRepository;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final PriceService priceService;
    public void applyStrategies(Long chatId, List<ProfitablePair> pairs) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        TradeStrategy strat = registry.getByType(settings.getStrategy());
        log.info("StrategyFacade ▶ chatId={} using strategy={}", chatId, settings.getStrategy());

        Duration interval = parseDuration(settings.getTimeframe());
        for (ProfitablePair pair : pairs) {
            String symbol = pair.getSymbol();
            log.info("StrategyFacade ▶ processing symbol={} for chatId={}", symbol, chatId);

            List<Candle> candles = candleService.history(symbol, interval, settings.getCachedCandlesLimit());
            if (candles == null || candles.isEmpty()) {
                log.warn("StrategyFacade ▶ no candles for symbol={} chatId={}", symbol, chatId);
                continue;
            }

            double lastClose = candles.get(candles.size() - 1).getClose();
            SignalType signal = strat.evaluate(candles, settings);
            log.info("StrategyFacade ▶ signal for symbol={} chatId={} → {}", symbol, chatId, signal);

            switch (signal) {
                case BUY -> enterTradeWithLock(chatId, symbol, lastClose, settings, pair);
                case SELL -> exitAllTrades(chatId, symbol, lastClose);
                case HOLD -> log.debug("StrategyFacade ▶ HOLD for symbol={} chatId={}", symbol, chatId);
            }
        }
    }

    private void enterTradeWithLock(Long chatId, String symbol, double price,
                                    AiTradingSettings settings, ProfitablePair pair) {
        String lockKey = chatId + ":" + symbol;
        ReentrantLock lock = tradeLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.warn("enterTrade ▶ another trade in progress for chatId={} symbol={}, skipping", chatId, symbol);
            return;
        }
        try {
            doEnterTrade(chatId, symbol, price, settings, pair);
        } finally {
            lock.unlock();
            tradeLocks.remove(lockKey, lock);
        }
    }

    private void doEnterTrade(Long chatId, String symbol, double entryPrice,
                              AiTradingSettings settings, ProfitablePair pair) {
        // Не открываем повторную позицию, если есть незакрытая
        boolean hasOpen = tradeLogRepository.existsByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        if (hasOpen) {
            log.warn("doEnterTrade ▶ already has open trade chatId={} symbol={}, skipping", chatId, symbol);
            return;
        }

        String quoteAsset = detectQuoteAsset(symbol);
        double freeBalance = accountService.getFreeBalance(chatId, quoteAsset);
        log.debug("enterTrade ▶ freeBalance={} {}", freeBalance, quoteAsset);
        if (freeBalance <= 0) {
            log.warn("enterTrade ▶ no balance for {} (chatId={})", quoteAsset, chatId);
            return;
        }

        double riskPct = settings.getRiskThreshold() != null ? settings.getRiskThreshold() : 0.0;
        BigDecimal amountToSpend = BigDecimal.valueOf(freeBalance)
                .multiply(BigDecimal.valueOf(riskPct / 100.0));
        if (amountToSpend.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("enterTrade ▶ zero risk amount: freeBalance={} riskPct={}", freeBalance, riskPct);
            return;
        }

        double slipPct = settings.getSlippageTolerance() != null ? settings.getSlippageTolerance() : 0.0;
        BigDecimal slipFactor = BigDecimal.valueOf(1.0 - slipPct / 100.0);
        BigDecimal spendable = amountToSpend.multiply(slipFactor);
        log.debug("enterTrade ▶ spendable after slippage {}%", slipPct);

        BigDecimal rawQty = spendable.divide(BigDecimal.valueOf(entryPrice), 8, RoundingMode.DOWN);
        BigDecimal stepSize = exchangeInfoService.getLotSizeStep(symbol);
        BigDecimal qtyBd = rawQty.divide(stepSize, 0, RoundingMode.DOWN).multiply(stepSize);
        if (qtyBd.compareTo(stepSize) < 0) {
            log.warn("enterTrade ▶ qty < stepSize after truncation: rawQty={} stepSize={}", rawQty, stepSize);
            return;
        }

        double qty = qtyBd.doubleValue();
        log.info("enterTrade ▶ calculated qty={} (stepSize={})", qty, stepSize);

        try {
            orderService.placeMarketOrder(chatId, symbol, qty);
            log.info("enterTrade ▶ Market BUY placed chatId={} symbol={} qty={}", chatId, symbol, qty);
        } catch (Exception e) {
            log.error("enterTrade ▶ Market order failed for chatId={} symbol={} qty={}: {}", chatId, symbol, qty, e.getMessage());
            return;
        }

        BigDecimal rawTp = BigDecimal.valueOf(entryPrice).multiply(BigDecimal.valueOf(1.0 + riskPct / 100.0));
        BigDecimal rawSl = BigDecimal.valueOf(entryPrice).multiply(BigDecimal.valueOf(1.0 - riskPct / 100.0));
        BigDecimal tickSize = exchangeInfoService.getPriceTickSize(symbol);
        BigDecimal tpBd = rawTp.divide(tickSize, 0, RoundingMode.DOWN).multiply(tickSize);
        BigDecimal slBd = rawSl.divide(tickSize, 0, RoundingMode.DOWN).multiply(tickSize);
        double tpPrice = tpBd.doubleValue();
        double slPrice = slBd.doubleValue();

        log.info("enterTrade ▶ TP={} SL={}", tpPrice, slPrice);

        try {
            orderService.placeOcoOrder(chatId, symbol, qty, slPrice, tpPrice);
            log.info("enterTrade ▶ OCO SELL placed chatId={} symbol={} qty={} SL={} TP={}",
                    chatId, symbol, qty, slPrice, tpPrice);
        } catch (Exception e) {
            log.warn("enterTrade ▶ OCO order failed for chatId={} symbol={} qty={} SL={} TP={}: {}",
                    chatId, symbol, qty, slPrice, tpPrice, e.getMessage());
        }

        TradeLog entry = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(entryPrice)
                .quantity(qty)
                .takeProfitPrice(tpPrice)
                .stopLossPrice(slPrice)
                .isClosed(false)
                .build();
        tradeLogRepository.save(entry);
        log.info("enterTrade ▶ trade recorded chatId={} symbol={} qty={} entryPrice={}",
                chatId, symbol, qty, entryPrice);
    }

    private void exitAllTrades(Long chatId, String symbol, double exitPrice) {
        List<TradeLog> openTrades = tradeLogRepository.findAllByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        if (openTrades.isEmpty()) return;

        // Отсортировать по потенциальной прибыли (выше — лучше)
        openTrades.sort((a, b) -> {
            double pnlA = (exitPrice - a.getEntryPrice()) * a.getQuantity();
            double pnlB = (exitPrice - b.getEntryPrice()) * b.getQuantity();
            return Double.compare(pnlB, pnlA);
        });

        for (TradeLog t : openTrades) {
            double pnl = (exitPrice - t.getEntryPrice()) * t.getQuantity();
            if (pnl <= 0) {
                log.info("exitAllTrades ▶ skipping non-profitable trade chatId={} symbol={} PnL={}", chatId, symbol, pnl);
                continue;
            }

            try {
                orderService.placeMarketOrder(chatId, symbol, t.getQuantity());
                log.info("exitAllTrades ▶ Market SELL placed chatId={} symbol={} qty={}", chatId, symbol, t.getQuantity());
            } catch (Exception e) {
                log.error("exitAllTrades ▶ sell failed chatId={} symbol={} qty={}: {}", chatId, symbol, t.getQuantity(), e.getMessage());
                continue;
            }

            t.setExitTime(Instant.now());
            t.setExitPrice(exitPrice);
            t.setPnl(pnl);
            t.setClosed(true);
            tradeLogRepository.save(t);
            log.info("exitAllTrades ▶ trade closed chatId={} symbol={} qty={} exitPrice={} PnL={}",
                    chatId, symbol, t.getQuantity(), exitPrice, pnl);
        }
    }

    private String detectQuoteAsset(String symbol) {
        if (symbol.endsWith("USDT")) return "USDT";
        if (symbol.endsWith("BUSD")) return "BUSD";
        if (symbol.endsWith("BTC")) return "BTC";
        if (symbol.endsWith("ETH")) return "ETH";
        return symbol.replaceFirst("^[A-Z]+", "");
    }

    private Duration parseDuration(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return Duration.ofMinutes(1);
        }
        try {
            timeframe = timeframe.trim().toLowerCase();
            if (timeframe.endsWith("m")) return Duration.ofMinutes(Integer.parseInt(timeframe.replace("m", "")));
            if (timeframe.endsWith("h")) return Duration.ofHours(Integer.parseInt(timeframe.replace("h", "")));
            if (timeframe.endsWith("d")) return Duration.ofDays(Integer.parseInt(timeframe.replace("d", "")));
        } catch (NumberFormatException e) {
            log.warn("parseDuration ▶ invalid timeframe='{}', defaulting to 1m", timeframe);
        }
        return Duration.ofMinutes(1);
    }
    public void exitTrade(Long chatId, String symbol) {
        BigDecimal price = priceService.getPrice(symbol);
        if (price == null) {
            log.warn("exitTrade ▶ current price unavailable for {} chatId={}", symbol, chatId);
            return;
        }
        exitAllTrades(chatId, symbol, price.doubleValue());
    }

}
