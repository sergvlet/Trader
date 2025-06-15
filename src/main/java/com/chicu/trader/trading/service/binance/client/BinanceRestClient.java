package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
public class BinanceRestClient {

    private final String apiKey;
    private final String secretKey;
    private final boolean isTestnet;
    private final BinanceHttpClient httpClient;

    public ExchangeInfo getExchangeInfo() {
        return httpClient.getExchangeInfo();
    }

    public BigDecimal getLastPrice(String symbol) {
        return httpClient.getLastPrice(symbol);
    }

    public BigDecimal getBalance(String asset) {
        return httpClient.getBalance(asset);
    }

    public void placeMarketBuy(String symbol, BigDecimal quantity) {
        httpClient.placeMarketBuy(symbol, quantity);
    }

    public void placeMarketSell(String symbol, BigDecimal quantity) {
        httpClient.placeMarketSell(symbol, quantity);
    }

    public void placeOcoSell(String symbol, BigDecimal qty, BigDecimal stopLossPrice, BigDecimal takeProfitPrice) {
        httpClient.placeOcoSell(symbol, qty, stopLossPrice, takeProfitPrice);
    }
}
