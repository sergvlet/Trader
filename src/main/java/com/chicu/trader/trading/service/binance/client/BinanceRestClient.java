package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.util.Map;

public class BinanceRestClient {

    private final BinanceHttpClient http;

    /**
     * Вызывается из BinanceRestClientFactory:
     * @param apiKey     — ключ пользователя (или null для публичного клиента)
     * @param secretKey  — секрет пользователя (или null для публичного клиента)
     * @param isTestnet  — флаг тестовой сети (определяет URL)
     * @param httpClient — уже сконфигурированный HTTP-клиент
     */
    public BinanceRestClient(String apiKey, String secretKey, boolean isTestnet, BinanceHttpClient httpClient) {
        this.http = httpClient;
    }

    public ExchangeInfo getExchangeInfo() {
        return http.getExchangeInfo();
    }

    public BigDecimal getLastPrice(String symbol) {
        return http.getLastPrice(symbol);
    }

    public BigDecimal getBalance(String asset) {
        return http.getBalance(asset);
    }

    /**
     * RAW JSON ответа от Binance для MARKET BUY
     */
    public String placeMarketBuyRaw(String symbol, BigDecimal quantity) {
        return http.placeMarketBuy(symbol, quantity);
    }

    /**
     * RAW JSON ответа от Binance для MARKET SELL
     */
    public String placeMarketSellRaw(String symbol, BigDecimal quantity) {
        return http.placeMarketSell(symbol, quantity);
    }

    /**
     * RAW JSON ответа от Binance для OCO SELL
     */
    public String placeOcoSellRaw(String symbol,
                                  BigDecimal quantity,
                                  BigDecimal stopLossPrice,
                                  BigDecimal takeProfitPrice) {
        return http.placeOcoSell(symbol, quantity, stopLossPrice, takeProfitPrice);
    }
    public String getOcoStatusRaw(String symbol, String listId) {
        // просто делегируем в HTTP-клиент
        return http.sendSigned(
                HttpMethod.GET,
                "/api/v3/orderList",
                Map.of("symbol", symbol, "listId", listId)
        );
    }
}
