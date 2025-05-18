// src/main/java/com/chicu/trader/trading/TradingEngine.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.model.TradeLog;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.repository.TradeLogRepository;
import com.chicu.trader.trading.context.StrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradingEngine {

    private final CandleService candleService;
    private final StrategyFacade strategyFacade;
    private final OrderService orderService;
    private final RiskManager riskManager;
    private final ProfitablePairRepository pairRepo;
    private final TradeLogRepository logRepo;

    // хранит подписки по chatId
    private final Map<Long, Disposable> streams = new ConcurrentHashMap<>();

    /**
     * Запускает автоторговлю: подписывается на часовые свечи,
     * строит контексты и обрабатывает каждый.
     */
    public void startAutoTrading(Long chatId) {
        List<ProfitablePair> pairs = pairRepo.findByUserChatIdAndActiveTrue(chatId);
        if (pairs.isEmpty()) {
            log.warn("User {} has no active pairs, cannot start trading", chatId);
            return;
        }

        strategyFacade.loadModel();

        Flux<StrategyContext> stream = candleService
                .streamHourly(chatId, pairs)
                .map(candle -> strategyFacade.buildContext(chatId, candle, pairs));

        Disposable sub = stream.subscribe(this::onNewCandle);
        streams.put(chatId, sub);

        log.info("Auto-trading started for user {}", chatId);
    }

    /**
     * Останавливает автоторговлю для пользователя.
     */
    public void stopAutoTrading(Long chatId) {
        Disposable sub = streams.remove(chatId);
        if (sub != null && !sub.isDisposed()) {
            sub.dispose();
            log.info("Auto-trading stopped for user {}", chatId);
        }
    }

    /**
     * Обработка каждой новой свечи в контексте стратегии.
     */
    private void onNewCandle(StrategyContext ctx) {
        Long chatId = ctx.getChatId();

        // Блокировка при просадке
        if (!riskManager.allowNewTrades(chatId)) {
            return;
        }

        // Открытие позиции
        if (strategyFacade.shouldEnter(ctx)) {
            TradeLog entryLog = orderService.openPosition(ctx);
            logRepo.save(entryLog);
            log.info("Opened position for {} @ {}", ctx.getSymbol(), ctx.getPrice());
        }

        // Закрытие позиции (TP/SL или UpperBB)
        orderService.checkAndClose(ctx)
                .ifPresent(exitLog -> {
                    logRepo.save(exitLog);
                    log.info("Closed position for {} @ {}", ctx.getSymbol(), exitLog.getExitPrice());
                });
    }
}
