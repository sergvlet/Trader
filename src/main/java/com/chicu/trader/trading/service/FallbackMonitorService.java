package com.chicu.trader.trading.service;

import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.StrategyFacade;
import com.chicu.trader.trading.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackMonitorService {

    private final TradeLogRepository tradeLogRepository;
    private final PriceService priceService;
    private final StrategyFacade strategyFacade;

    @Scheduled(fixedRate = 15_000)
    public void monitorOpenPositions() {
        List<TradeLog> openTrades = tradeLogRepository.findAllByIsClosedFalse();

        for (TradeLog trade : openTrades) {
            BigDecimal currentPrice = priceService.getPrice(trade.getSymbol());
            BigDecimal tp = BigDecimal.valueOf(trade.getTakeProfitPrice());
            BigDecimal sl = BigDecimal.valueOf(trade.getStopLossPrice());

            if (currentPrice == null || tp == null || sl == null) continue;

            if (currentPrice.compareTo(tp) >= 0 || currentPrice.compareTo(sl) <= 0) {
                log.warn("⚠️ [Fallback TP/SL] Closing trade by fallback for {}: cur={}, TP={}, SL={}",
                        trade.getSymbol(), currentPrice, tp, sl);
                strategyFacade.exitTrade(trade.getUserChatId(), trade.getSymbol());
            }
        }
    }
}
