// src/main/java/com/chicu/trader/trading/MlTrainer.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.DailyOptimizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MlTrainer {

    private final DailyOptimizer dailyOptimizer;

    /**
     * Синхронно запускает переобучение ML-модели для данного chatId.
     * Блокирует поток до завершения.
     */
    public void trainNow(Long chatId) {
        // вызывает вашу логику из DailyOptimizer,
        // но без сна в 03:00, сразу «по требованию».
        dailyOptimizer.optimizeForChat(chatId);
    }
}
