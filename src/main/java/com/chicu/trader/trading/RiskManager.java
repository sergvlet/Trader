// src/main/java/com/chicu/trader/trading/RiskManager.java
package com.chicu.trader.trading;

import com.chicu.trader.trading.service.AccountService;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskManager {

    private final AccountService              accountService;
    private final AiTradingSettingsService    settingsService;

    /**
     * Рассчитать количество лотов для открытия позиции,
     * исходя из % баланса на сделку из настроек AI.
     *
     * @param chatId  чат пользователя
     * @param symbol  торговая пара, например "BTCUSDT"
     * @param price   текущая цена
     * @return количество единиц базового актива, которое можно купить
     */
    public double calculatePositionSize(Long chatId, String symbol, double price) {
        // Получаем баланс базового актива (например, USDT)
        String baseAsset = symbol.replaceAll("[A-Z]+$", "");
        double balance = accountService.getFreeBalance(chatId, baseAsset);

        // Читаем % от депозита на сделку из настроек AI (например, 1.0 = 1%)
        double riskPct = Optional.ofNullable(
                settingsService.getOrCreate(chatId).getRiskThreshold()
        ).orElse(1.0) / 100.0;

        double alloc = balance * riskPct;
        double quantity = alloc / price;

        log.debug("Расчет размера позиции chatId={} symbol={} balance={} riskPct={} alloc={} quantity={}",
                chatId, symbol, balance, riskPct, alloc, quantity);

        return quantity;
    }
}
