package com.chicu.trader.trading.provider;

import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.provider.MarketDataProvider;
import com.chicu.trader.trading.service.binance.HttpBinanceCandleService;
import com.chicu.trader.trading.service.binance.BinanceExchangeInfoService;
import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BinanceMarketDataProvider implements MarketDataProvider {

    private final HttpBinanceCandleService candleService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final BinanceRestClientFactory clientFactory;

    @Override
    public List<Candle> fetchCandles(String symbol, Duration timeframe, int limit) {
        return candleService.fetchCandles(symbol, timeframe, limit);
    }

    @Override
    public List<String> getAllSymbols() {
        // Получаем кэшированный ExchangeInfo и извлекаем названия всех символов
        return exchangeInfoService.getExchangeInfoCached()
                .getSymbols().stream()
                .map(SymbolInfo::getSymbol)
                .filter(s -> s.endsWith("USDT"))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getCurrentPrice(Long chatId, String symbol) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        return client.getLastPrice(symbol);
    }

    /**
     * Получить публичную цену без API-ключей.
     */
    public BigDecimal getPublicPrice(String symbol) {
        BinanceRestClient publicClient = clientFactory.getPublicClient();
        return publicClient.getLastPrice(symbol);
    }
}
