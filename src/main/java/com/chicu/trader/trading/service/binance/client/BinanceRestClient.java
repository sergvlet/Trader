package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.util.Map;

public class BinanceRestClient {

    private final BinanceHttpClient http;

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

    public Map<String, BinanceHttpClient.BalanceInfo> getFullBalance() {
        return http.getFullBalance();
    }

    public String placeMarketBuyRaw(String symbol, BigDecimal quantity) {
        return http.placeMarketBuy(symbol, quantity);
    }

    public String placeMarketSellRaw(String symbol, BigDecimal quantity) {
        return http.placeMarketSell(symbol, quantity);
    }

    public String placeOcoSellRaw(String symbol,
                                  BigDecimal quantity,
                                  BigDecimal stopLossPrice,
                                  BigDecimal takeProfitPrice) {
        return http.placeOcoSell(symbol, quantity, stopLossPrice, takeProfitPrice);
    }

    public String getOcoStatusRaw(String symbol, String listId) {
        return http.sendSigned(
                HttpMethod.GET,
                "/api/v3/orderList",
                Map.of("symbol", symbol, "listId", listId)
        );
    }
}
