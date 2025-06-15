package com.chicu.trader.trading.service.binance;

import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BinanceRestClientFactory clientFactory;

    /**
     * Рыночная покупка
     */
    public void placeMarketBuy(Long chatId, String symbol, BigDecimal quantity) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        log.info("OrderService: размещаем MARKET BUY для {} qty={}", symbol, quantity);
        client.placeMarketBuy(symbol, quantity);
    }

    /**
     * Рыночная продажа
     */
    public void placeMarketSell(Long chatId, String symbol, BigDecimal quantity) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        log.info("OrderService: размещаем MARKET SELL для {} qty={}", symbol, quantity);
        client.placeMarketSell(symbol, quantity);
    }

    /**
     * OCO продажа
     */
    public void placeOcoSell(Long chatId, String symbol, BigDecimal quantity, BigDecimal stopLossPrice, BigDecimal takeProfitPrice) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        log.info("OrderService: размещаем OCO SELL для {} qty={} SL={} TP={}", symbol, quantity, stopLossPrice, takeProfitPrice);
        client.placeOcoSell(symbol, quantity, stopLossPrice, takeProfitPrice);
    }

    /**
     * Получение баланса
     */
    public BigDecimal getBalance(Long chatId, String asset) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        return client.getBalance(asset);
    }

    /**
     * Получение текущей цены
     */
    public BigDecimal getLastPrice(Long chatId, String symbol) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        return client.getLastPrice(symbol);
    }
}
