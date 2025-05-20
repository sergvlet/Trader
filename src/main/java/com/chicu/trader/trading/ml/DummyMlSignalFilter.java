package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2) // менее приоритетный
public class DummyMlSignalFilter implements MlSignalFilter {

    @Override
    public MarketSignal predict(Long chatId, MarketData data) {
        log.warn("🔁 Dummy-фильтр: всегда BUY для chatId={}", chatId);
        return MarketSignal.BUY;
    }
}
