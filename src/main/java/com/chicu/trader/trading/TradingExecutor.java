// src/main/java/com/chicu/trader/trading/TradingExecutor.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.binance.HttpBinanceWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradingExecutor {

    private final ProfitablePairRepository     pairRepo;
    private final StrategyFacade               strategyFacade;
    private final HttpBinanceWebSocketService  wsService;

    /**
     * Вызывается WebSocket‐сервисом при каждом закрытом баре.
     */
    private void handleCandle(Long chatId, Candle candle) {
        List<ProfitablePair> pairs = pairRepo.findByUserChatIdAndActiveTrue(chatId);
        if (pairs.isEmpty()) {
            return;
        }

        try {
            strategyFacade.applyStrategies(chatId, candle, pairs);
        } catch (Exception ex) {
            log.error("Ошибка исполнения стратегии для chatId={}, symbol={}", chatId, candle.getSymbol(), ex);
        }
    }

    /**
     * Подписаться на WS и начать обрабатывать закрытые свечи.
     */
    public void startExecutor(Long chatId, List<String> symbols) {
        wsService.startSubscriptions(chatId, symbols, candle -> handleCandle(chatId, candle));
        log.info("Торговый исполнитель запущен для chatId={} symbols={}", chatId, symbols);
    }

    /**
     * Остановить все подписки.
     */
    public void stopExecutor() {
        wsService.stopSubscriptions();
        log.info("Торговый исполнитель остановлен");
    }

    /**
     * Перезапустить подписки с новым списком символов.
     */
    public void updateExecutor(Long chatId, List<String> newSymbols) {
        wsService.stopSubscriptions(chatId);
        wsService.startSubscriptions(chatId, newSymbols, candle -> handleCandle(chatId, candle));
        log.info("Перезапущен торговый исполнитель для chatId={} с новыми парами: {}", chatId, newSymbols);
    }
}
