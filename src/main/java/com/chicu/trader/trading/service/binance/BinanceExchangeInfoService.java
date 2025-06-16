package com.chicu.trader.trading.service.binance;

import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolFilter;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceExchangeInfoService {

    private final BinanceRestClientFactory clientFactory;
    private BinanceRestClient publicClient;
    private volatile ExchangeInfo exchangeInfo;
    private final Map<String, BigDecimal> priceTickSizeMap = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lotStepSizeMap   = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.publicClient = clientFactory.getPublicClient();
        try {
            refresh();
        } catch (Exception e) {
            log.warn("Не удалось загрузить Binance ExchangeInfo на старте, попробую при первом запросе", e);
        }
    }

    public synchronized void refresh() {
        log.info("Refreshing Binance ExchangeInfo…");
        ExchangeInfo newInfo = publicClient.getExchangeInfo();

        Map<String, BigDecimal> newPriceMap = new ConcurrentHashMap<>();
        Map<String, BigDecimal> newLotMap   = new ConcurrentHashMap<>();

        for (SymbolInfo si : newInfo.getSymbols()) {
            String symbol = si.getSymbol();
            BigDecimal tickSize = si.getFilters().stream()
                .filter(f -> "PRICE_FILTER".equals(f.getFilterType()))
                .map(SymbolFilter::getTickSize)
                .map(BigDecimal::new)
                .findFirst()
                .orElse(BigDecimal.ZERO);

            BigDecimal stepSize = si.getFilters().stream()
                .filter(f -> "LOT_SIZE".equals(f.getFilterType()))
                .map(SymbolFilter::getStepSize)
                .map(BigDecimal::new)
                .findFirst()
                .orElse(BigDecimal.ZERO);

            newPriceMap.put(symbol, tickSize);
            newLotMap  .put(symbol, stepSize);
        }

        this.exchangeInfo       = newInfo;
        this.priceTickSizeMap.clear();
        this.priceTickSizeMap.putAll(newPriceMap);
        this.lotStepSizeMap.clear();
        this.lotStepSizeMap.putAll(newLotMap);

        log.info("Binance ExchangeInfo refreshed: {} symbols", newPriceMap.size());
    }

    public ExchangeInfo getExchangeInfoCached() {
        if (exchangeInfo == null) {
            synchronized (this) {
                if (exchangeInfo == null) {
                    try {
                        refresh();
                    } catch (Exception e) {
                        log.error("Не удалось загрузить Binance ExchangeInfo при on-demand запросе", e);
                        ExchangeInfo empty = new ExchangeInfo();
                        empty.setSymbols(Collections.emptyList());
                        return empty;
                    }
                }
            }
        }
        return exchangeInfo;
    }

    public BigDecimal getPriceTickSize(String symbol) {
        return priceTickSizeMap.getOrDefault(symbol, BigDecimal.ZERO);
    }

    public BigDecimal getLotStepSize(String symbol) {
        return lotStepSizeMap.getOrDefault(symbol, BigDecimal.ZERO);
    }
}
