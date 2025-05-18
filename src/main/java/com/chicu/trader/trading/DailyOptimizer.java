// src/main/java/com/chicu/trader/trading/DailyOptimizer.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.ml.MlModelTrainer;
import com.chicu.trader.trading.optimizer.TpSlOptimizer;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyOptimizer {

    private final CandleService candleService;
    private final ProfitablePairRepository pairRepo;
    private final TpSlOptimizer optimizer;
    private final MlModelTrainer modelTrainer;

    /**
     * Ежедневная оптимизация TP/SL и переобучение модели.
     * Запускается в 03:00 Europe/Warsaw.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Warsaw")
    public void nightlyUpdate() {
        List<ProfitablePair> pairs = pairRepo.findAll();
        for (ProfitablePair p : pairs) {
            Long chatId = p.getUserChatId();
            String symbol = p.getSymbol();

            // Сбор исторических данных
            List<Candle> hist120 = candleService.historyHourly(chatId, symbol, 120);

            // Оптимизация TP/SL на этой истории
            TpSlOptimizer.Result r = optimizer.optimize(hist120);

            // Сохраняем результаты
            p.setTakeProfitPct(r.getTpPct());
            p.setStopLossPct(r.getSlPct());
            p.setActive(r.getProfit() > 0);
            pairRepo.save(p);
        }

        // Переобучаем ML-модель и экспортируем ONNX
        modelTrainer.trainAndExport("models/ml_signal_filter.onnx");
    }
}
