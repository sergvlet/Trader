package com.chicu.trader.bot.service;

import com.chicu.trader.dto.BinancePairDto;

import java.util.List;

public interface BinancePairService {

    /**
     * Возвращает список всех доступных торговых пар с их ценой и изменением.
     * Учитывает режим (testnet / mainnet) для фильтрации.
     *
     * @param isTestnet использовать ли тестовую сеть Binance
     * @return список DTO с информацией о парах
     */
    List<BinancePairDto> getAllAvailablePairs(boolean isTestnet);
}
