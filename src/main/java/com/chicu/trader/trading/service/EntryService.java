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

    /**
     * Открывает MARKET BUY и ставит OCO-ордер под заданные параметры.
     */
    public void enterTrade(Long chatId,
                           String symbol,
                           double entryPrice,
                           AiTradingSettings settings,
                           ProfitablePair pair) {

        // 1) Проверяем, нет ли уже открытой сделки по этому символу
        if (tradeLogRepository.existsByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol)) {
            log.warn("Entry ▶ already open trade chatId={} symbol={}", chatId, symbol);
            return;
        }

        // 2) Смотрим свободный баланс в квоте
        String quoteAsset = detectQuoteAsset(symbol);
        BigDecimal freeBalance = accountService.getFreeBalance(chatId, quoteAsset);
        if (freeBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Entry ▶ no free {} balance for chatId={}", quoteAsset, chatId);
            return;
        }

        // 3) Считаем, сколько потратить (riskThreshold в %)
        double riskPct = settings.getRiskThreshold() != null ? settings.getRiskThreshold() : 0.0;
        BigDecimal toSpend = freeBalance.multiply(BigDecimal.valueOf(riskPct / 100.0));
        if (toSpend.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Entry ▶ zero amount to spend: chatId={} symbol={}", chatId, symbol);
            return;
        }

        // 4) Учёт проскальзывания
        double slipPct = settings.getSlippageTolerance() != null
                ? settings.getSlippageTolerance()
                : 0.0;
        BigDecimal spendable = toSpend.multiply(
                BigDecimal.valueOf(1.0 - slipPct / 100.0)
        );

        // 5) Считаем «сырое» количество
        BigDecimal rawQty = spendable.divide(
                BigDecimal.valueOf(entryPrice),
                8,
                RoundingMode.DOWN
        );

        // 6) Выравниваем по шагу лота
        BigDecimal stepSize = exchangeInfoService.getLotStepSize(symbol);
        if (stepSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Entry ▶ stepSize=0 for symbol={}", symbol);
            return;
        }
        int qtyScale = stepSize.stripTrailingZeros().scale();
        BigDecimal qtyBd = rawQty.setScale(qtyScale, RoundingMode.DOWN);
        if (qtyBd.compareTo(stepSize) < 0) {
            log.warn("Entry ▶ qty {} < stepSize {} for symbol={}", qtyBd, stepSize, symbol);
            return;
        }

        // 7) Размещаем MARKET BUY
        try {
            orderService.placeMarketBuy(chatId, symbol, qtyBd);
            log.info("Entry ▶ MARKET BUY placed: chatId={} symbol={} qty={}",
                    chatId, symbol, qtyBd);
        } catch (Exception e) {
            log.error("Entry ▶ failed to place MARKET BUY: chatId={} symbol={} qty={} — {}",
                    chatId, symbol, qtyBd, e.getMessage(), e);
            return;
        }

        // 8) Ставим OCO (TP/SL) по параметрам ProfitablePair
        BigDecimal tickSize = exchangeInfoService.getPriceTickSize(symbol);
        if (tickSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Entry ▶ tickSize=0 for symbol={}", symbol);
        } else {
            // takeProfit = entryPrice * (1 + takeProfitPct/100)
            BigDecimal tp = BigDecimal.valueOf(entryPrice)
                    .multiply(BigDecimal.valueOf(1.0 + pair.getTakeProfitPct() / 100.0))
                    .divide(tickSize, 0, RoundingMode.DOWN)
                    .multiply(tickSize);

            // stopLoss = entryPrice * (1 - stopLossPct/100)
            BigDecimal sl = BigDecimal.valueOf(entryPrice)
                    .multiply(BigDecimal.valueOf(1.0 - pair.getStopLossPct() / 100.0))
                    .divide(tickSize, 0, RoundingMode.DOWN)
                    .multiply(tickSize);

            try {
                orderService.placeOcoSell(chatId, symbol, qtyBd, sl, tp);
                log.info("Entry ▶ OCO placed: chatId={} symbol={} SL={} TP={}",
                        chatId, symbol, sl, tp);
            } catch (Exception e) {
                log.warn("Entry ▶ failed to place OCO: {}", e.getMessage(), e);
            }
        }

        // 9) Сохраняем в TradeLog
        TradeLog logEntry = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(entryPrice)
                .quantity(qtyBd.doubleValue())
                .takeProfitPrice(
                        BigDecimal.valueOf(entryPrice)
                                .multiply(BigDecimal.valueOf(1.0 + pair.getTakeProfitPct() / 100.0))
                                .setScale(tickSize.stripTrailingZeros().scale(), RoundingMode.DOWN)
                )
                .stopLossPrice(
                        BigDecimal.valueOf(entryPrice)
                                .multiply(BigDecimal.valueOf(1.0 - pair.getStopLossPct() / 100.0))
                                .setScale(tickSize.stripTrailingZeros().scale(), RoundingMode.DOWN)
                )
                .isClosed(false)
                .build();
        tradeLogRepository.save(logEntry);

        log.info("Entry ▶ TradeLog saved: chatId={} symbol={} qty={} entry={}",
                chatId, symbol, qtyBd, entryPrice);
    }

    private String detectQuoteAsset(String symbol) {
        if (symbol.endsWith("USDT")) return "USDT";
        if (symbol.endsWith("BUSD")) return "BUSD";
        if (symbol.endsWith("BTC"))  return "BTC";
        if (symbol.endsWith("ETH"))  return "ETH";
        return symbol.replaceFirst("^[A-Z]+", "");
    }
}
