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
        optimizeForChat(null);
    }

    /**
     * Оптимизировать TP/SL и модель для одного chatId.
     * Если chatId == null — для всех.
     */
    public void optimizeForChat(Long chatId) {
        List<ProfitablePair> pairs =
                (chatId == null)
                        ? pairRepo.findAll()
                        : pairRepo.findByUserChatId(chatId);

        for (ProfitablePair p : pairs) {
            Long uid = p.getUserChatId();
            String symbol = p.getSymbol();

            // 1) история
            List<Candle> hist120 = candleService.historyHourly(uid, symbol, 120);

            // 2) TP/SL
            TpSlOptimizer.Result r = optimizer.optimize(hist120);

            p.setTakeProfitPct(r.getTpPct());
            p.setStopLossPct(r.getSlPct());
            p.setActive(r.getProfit() > 0);
            pairRepo.save(p);
        }

        // 3) ML
        // если нужно обучать на отдельных чатах — передайте chatId
        modelTrainer.trainAndExport("models/ml_signal_filter.onnx");
    }
}
