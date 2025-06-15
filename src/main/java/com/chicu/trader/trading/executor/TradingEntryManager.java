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

    public void enterTrade(Long chatId, String symbol, double price, AiTradingSettings settings) {
        // Проверяем есть ли уже открытая сделка
        boolean hasOpen = tradeLogRepository.existsByUserChatIdAndSymbolAndIsClosedFalse(chatId, symbol);
        if (hasOpen) {
            log.info("⛔ Уже есть открытая сделка по {} для chatId={}", symbol, chatId);
            return;
        }

        // Получаем размер позиции от RiskManager
        double qty = riskManager.calculatePositionSize(chatId, symbol, price, settings);
        if (qty <= 0) {
            log.warn("⚠ Нулевая позиция для входа: chatId={} symbol={}", chatId, symbol);
            return;
        }

        // Округляем под stepSize
        BigDecimal stepSize = exchangeInfoService.getLotSizeStep(symbol);
        BigDecimal qtyRounded = BigDecimal.valueOf(qty).divide(stepSize, 0, RoundingMode.DOWN).multiply(stepSize);
        double finalQty = qtyRounded.doubleValue();
        if (finalQty < stepSize.doubleValue()) {
            log.warn("⚠ Размер позиции меньше минимального лота: chatId={} symbol={}", chatId, symbol);
            return;
        }

        // Размещаем ордер
        try {
            orderService.placeMarketBuy(chatId, symbol, BigDecimal.valueOf(finalQty));
            log.info("✅ BUY market order placed: chatId={} symbol={} qty={}", chatId, symbol, finalQty);
        } catch (Exception e) {
            log.error("❌ Failed to place BUY order: chatId={} symbol={} qty={}: {}", chatId, symbol, finalQty, e.getMessage());
            return;
        }

        // Сохраняем сделку в лог
        TradeLog tradeLog = TradeLog.builder()
                .userChatId(chatId)
                .symbol(symbol)
                .entryTime(Instant.now())
                .entryPrice(price)
                .quantity(finalQty)
                .isClosed(false)
                .build();

        tradeLogRepository.save(tradeLog);
        log.info("💾 TradeLog записан: chatId={} symbol={} qty={} entry={}", chatId, symbol, finalQty, price);
    }
}
