package com.chicu.trader.trading.service.binance;

import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BinanceExchangeInfoService {

    private final BinanceRestClientFactory clientFactory;
    private volatile List<SymbolInfo> cachedSymbols;

    public synchronized void refresh() {
        BinanceRestClient client = clientFactory.getPublicClient();
        ExchangeInfo exchangeInfo = client.getExchangeInfo();
        cachedSymbols = exchangeInfo.getSymbols();
    }

    private List<SymbolInfo> getSymbols() {
        if (cachedSymbols == null) {
            refresh();
        }
        return cachedSymbols;
    }

    public List<String> getAllSymbols() {
        return getSymbols().stream()
                .map(SymbolInfo::getSymbol)
                .filter(symbol -> symbol.endsWith("USDT"))
                .collect(Collectors.toList());
    }

    public BigDecimal getLotSizeStep(String symbol) {
        return getSymbols().stream()
                .filter(s -> s.getSymbol().equals(symbol))
                .map(SymbolInfo::getLotSizeStep)
                .findFirst()
                .orElse(BigDecimal.valueOf(0.000001));
    }

    public BigDecimal getPriceTickSize(String symbol) {
        return getSymbols().stream()
                .filter(s -> s.getSymbol().equals(symbol))
                .map(SymbolInfo::getPriceTickSize)
                .findFirst()
                .orElse(BigDecimal.valueOf(0.000001));
    }
}
