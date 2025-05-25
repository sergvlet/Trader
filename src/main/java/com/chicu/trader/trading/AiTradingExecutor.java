//package com.chicu.trader.trading;
//
//import com.chicu.trader.bot.entity.AiTradingSettings;
//import com.chicu.trader.bot.service.AiTradingSettingsService;
//import com.chicu.trader.trading.indicator.EmaCalculator;
//import com.chicu.trader.trading.indicator.RsiCalculator;
//import com.chicu.trader.trading.model.CandleInterval;
//import com.chicu.trader.trading.model.CandleResponse;
//import com.chicu.trader.trading.model.NewOrderRequest;
//import com.chicu.trader.trading.model.NewOrderResponse;
//import com.chicu.trader.trading.service.binance.HttpBinanceCandleService;
//import com.chicu.trader.trading.service.binance.HttpBinanceOrderService;
//import com.chicu.trader.trading.service.binance.HttpBinanceSignedService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AiTradingExecutor {
//
//    private final AiTradingSettingsService settingsService;
//    private final HttpBinanceCandleService candleService;
//    private final HttpBinanceOrderService orderService;
//    private final HttpBinanceSignedService signedService;
//
//    public void execute(Long chatId) {
//        AiTradingSettings s = settingsService.getOrCreate(chatId);
//
//        if (!Boolean.TRUE.equals(s.getIsRunning())) {
//            log.info("⏸️ AI выключен для chatId={}, пропускаем", chatId);
//            return;
//        }
//
//        String symbol = getFirstSymbol(s);
//        CandleInterval interval = CandleInterval.fromString(s.getTimeframe());
//        List<CandleResponse> candles = candleService.getCandles(symbol, interval, 100);
//
//        if (candles.isEmpty()) {
//            log.warn("⛔ Нет свечей для пары {}", symbol);
//            return;
//        }
//
//        List<Double> closes = candles.stream()
//                .map(CandleResponse::getClose)
//                .map(BigDecimal::doubleValue)
//                .toList();
//
//        double rsi = RsiCalculator.calculate(closes, s.getRsiPeriod());
//        double emaShort = EmaCalculator.calculate(closes, s.getEmaShort());
//        double emaLong = EmaCalculator.calculate(closes, s.getEmaLong());
//
//        log.info("📉 Пара {} | RSI={} | EMA_S={} | EMA_L={}", symbol, rsi, emaShort, emaLong);
//
//        boolean buy = rsi < s.getRsiBuyThreshold() && emaShort > emaLong;
//        boolean sell = rsi > s.getRsiSellThreshold() && emaShort < emaLong;
//
//        if (!buy && !sell) {
//            log.info("⚠️ Условие входа не выполнено, сделка не открыта.");
//            return;
//        }
//
//        String side = buy ? "BUY" : "SELL";
//        NewOrderRequest req = NewOrderRequest.market(symbol, side, BigDecimal.valueOf(s.getTradeAmount()));
//
//        try {
//            NewOrderResponse resp = orderService.createOrder(chatId, req);
//            log.info("✅ Открыт ордер: {} {} {} → id={}", side, symbol, s.getTradeAmount(), resp.orderId());
//        } catch (Exception e) {
//            log.error("⛔ Ошибка при открытии ордера: {}", e.getMessage(), e);
//        }
//    }
//
//    private String getFirstSymbol(AiTradingSettings s) {
//        if (s.getManualPair() != null) return s.getManualPair();
//        if (s.getSymbols() == null || s.getSymbols().isBlank()) return "BTCUSDT";
//        return s.getSymbols().split(",")[0];
//    }
//}
