package com.chicu.trader.trading.service;

import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final BinanceRestClientFactory clientFactory;

    public BigDecimal getFreeBalance(Long chatId, String asset) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        return client.getBalance(asset);
    }
}
