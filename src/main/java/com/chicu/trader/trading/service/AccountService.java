package com.chicu.trader.trading.service;

import com.chicu.trader.trading.service.binance.HttpBinanceAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final HttpBinanceAccountService binanceAccountService;

    /**
     * Возвращает свободный баланс базового актива (например, "USDT") для данного пользователя.
     */
    public double getFreeBalance(Long chatId, String asset) {
        log.debug("Получение баланса для chatId={} asset={}", chatId, asset);
        Map<String, Double> balances = binanceAccountService.getBalances(chatId);
        return balances.getOrDefault(asset, 0.0);
    }
}
