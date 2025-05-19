// src/main/java/com/chicu/trader/trading/TradingEngine.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.event.TradingToggleEvent;
import com.chicu.trader.bot.service.MenuEditor;
import com.chicu.trader.model.TradeLog;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradingEngine {

    private final CandleService             candleService;
    private final StrategyFacade            strategyFacade;
    private final OrderService              orderService;
    private final RiskManager               riskManager;
    private final TradingStatusService      statusService;
    private final MenuEditor                menuEditor;
    private final MarketDataService         marketDataService;
    private final MlTrainer                 mlTrainer;
    private final ProfitablePairRepository  pairRepo;
    private final TradeLogRepository        logRepo;

    private final Map<Long, Disposable> streams = new ConcurrentHashMap<>();

    @EventListener
    public void onTradingToggle(TradingToggleEvent evt) {
        if (evt.isStart()) startAutoTrading(evt.getChatId());
        else               stopAutoTrading(evt.getChatId());
    }

    public void startAutoTrading(Long chatId) {
        List<String> symbols = marketDataService.getTopNLiquidPairs(5);
        if (symbols.isEmpty()) {
            statusService.setLastEvent(chatId, "❌ Не удалось получить пары");
            menuEditor.updateMenu(chatId, "ai_trading");
            return;
        }

        statusService.setLastEvent(chatId, "ℹ️ Переобучаю модель и TP/SL…");
        menuEditor.updateMenu(chatId, "ai_trading");
        try {
            mlTrainer.trainNow(chatId);
            statusService.setLastEvent(chatId, "✅ Модель и параметры обновлены");
        } catch (Exception ex) {
            log.error("Ошибка тренировки для {}", chatId, ex);
            statusService.setLastEvent(chatId, "⚠️ Ошибка обучения");
            menuEditor.updateMenu(chatId, "ai_trading");
            return;
        }
        menuEditor.updateMenu(chatId, "ai_trading");

        List<ProfitablePair> pairs = pairRepo.findByUserChatIdAndActiveTrue(chatId);
        Disposable sub = candleService
                .streamHourly(chatId, pairs)
                .map(c -> strategyFacade.buildContext(chatId, c, pairs))
                .subscribe(this::onNewCandle, err -> onError(chatId, err), () -> onComplete(chatId));
        streams.put(chatId, sub);

        statusService.markRunning(chatId);
        statusService.setLastEvent(chatId, "▶️ Торговля запущена на: " + String.join(", ", symbols));
        menuEditor.updateMenu(chatId, "ai_trading");
        log.info("Auto-trading started for {} on {}", chatId, symbols);
    }

    public void stopAutoTrading(Long chatId) {
        Disposable sub = streams.remove(chatId);
        if (sub != null && !sub.isDisposed()) sub.dispose();
        statusService.markStopped(chatId);
        statusService.setLastEvent(chatId, "⏹️ Торговля остановлена");
        menuEditor.updateMenu(chatId, "ai_trading");
        log.info("Auto-trading stopped for {}", chatId);
    }

    private void onNewCandle(StrategyContext ctx) {
        Long chatId = ctx.getChatId();
        if (!riskManager.allowNewTrades(chatId)) {
            statusService.setLastEvent(chatId, "⏸️ Пауза: просадка");
            menuEditor.updateMenu(chatId, "ai_trading");
            return;
        }

        if (strategyFacade.shouldEnter(ctx)) {
            log.info("Attempting entry for {} @ {}", ctx.getSymbol(), ctx.getPrice());
            TradeLog entry = orderService.openPosition(ctx);
            logRepo.save(entry);
            statusService.setLastEvent(chatId,
                    String.format("✅ Вход %s @ %.4f", ctx.getSymbol(), ctx.getPrice()));
            menuEditor.updateMenu(chatId, "ai_trading");
            log.info("Entry executed for {}: {}", chatId, entry);
        }

        orderService.checkAndClose(ctx).ifPresent(exitLog -> {
            log.info("Attempting exit for {} @ {}", exitLog.getSymbol(), exitLog.getExitPrice());
            logRepo.save(exitLog);
            statusService.setLastEvent(chatId,
                    String.format("🔒 Выход %s @ %.4f (PnL=%.4f)",
                            exitLog.getSymbol(), exitLog.getExitPrice(), exitLog.getPnl()));
            menuEditor.updateMenu(chatId, "ai_trading");
            log.info("Exit executed for {}: {}", chatId, exitLog);
        });
    }

    private void onError(Long chatId, Throwable err) {
        log.error("Stream error for {}", chatId, err);
        statusService.setLastEvent(chatId, "⚠️ Ошибка потока");
        menuEditor.updateMenu(chatId, "ai_trading");
    }

    private void onComplete(Long chatId) {
        log.info("Stream completed for {}", chatId);
        statusService.setLastEvent(chatId, "ℹ️ Поток завершён");
        menuEditor.updateMenu(chatId, "ai_trading");
    }
}
