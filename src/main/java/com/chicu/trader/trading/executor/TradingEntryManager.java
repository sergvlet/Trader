package com.chicu.trader.trading.executor;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.chicu.trader.trading.risk.RiskManager;
import com.chicu.trader.trading.service.binance.BinanceExchangeInfoService;
import com.chicu.trader.trading.service.binance.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingEntryManager {

    private final TradeLogRepository tradeLogRepository;
    private final OrderService orderService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final RiskManager riskManager;

    /**
     * Открывает MARKET BUY ордер, если нет открытой позиции по symbol.
     */
    public void enterTrade(Long chatId,
                           String symbol,
                           double price,
                           AiTradingSettings settings) {

        // 1) Проверяем, нет ли уже открытой сделки
        if (tradeLogRepository.existsByUserChatIdAndSymbolAndClosedFalse(chatId, symbol)) {
            log.info("⛔ Уже есть открытая сделка для {} (chatId={})", symbol, chatId);
            return;
        }

        // 2) Вычисляем "сырой" размер позиции у RiskManager
        double rawQty = riskManager.calculatePositionSize(chatId, symbol, price, settings);
        if (rawQty <= 0) {
            log.warn("⚠ Нулевая позиция для входа: chatId={} symbol={}", chatId, symbol);
            return;
        }

        // 3) Округляем rawQty по шагу лота (stepSize)
        BigDecimal stepSize = exchangeInfoService.getLotStepSize(symbol);
        BigDecimal qtyBd   = BigDecimal.valueOf(rawQty);
        if (stepSize.compareTo(BigDecimal.ZERO) > 0) {
            int scale = stepSize.stripTrailingZeros().scale();
            qtyBd = qtyBd.setScale(scale, RoundingMode.DOWN);
        }
        if (qtyBd.compareTo(stepSize) < 0) {
            log.warn("⚠ Размер позиции {} меньше минимального шага {} для symbol={}", qtyBd, stepSize, symbol);
            return;
        }

        // 4) Пытаемся разместить MARKET BUY
        try {
            orderService.placeMarketBuy(chatId, symbol, qtyBd);
            log.info("✅ Размещен MARKET BUY: chatId={} symbol={} qty={}", chatId, symbol, qtyBd);
        } catch (Exception ex) {
            log.error("❌ Ошибка размещения BUY: chatId={} symbol={} qty={} — {}",
                    chatId, symbol, qtyBd, ex.getMessage(), ex);
            return;
        }

        // 5) Логируем сделку в базе
        TradeLog logEntry = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(BigDecimal.valueOf(price))
                .quantity(qtyBd)
                .closed(false)                // здесь используем .closed(), а не .isClosed()
                .build();
        tradeLogRepository.save(logEntry);

        log.info("💾 Сохранен TradeLog: chatId={} symbol={} qty={} entry={}", chatId, symbol, qtyBd, price);
    }
}
