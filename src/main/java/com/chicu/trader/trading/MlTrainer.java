// src/main/java/com/chicu/trader/trading/MlTrainer.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.ml.MlModelTrainer;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.optimizer.TpSlOptimizer;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MlTrainer {

    private final CandleService             candleService;
    private final ProfitablePairRepository  pairRepo;
    private final TpSlOptimizer             optimizer;
    private final MlModelTrainer            modelTrainer;

    public void trainNow(Long chatId) {
        List<ProfitablePair> pairs = pairRepo.findByUserChatIdAndActiveTrue(chatId);
        for (ProfitablePair p : pairs) {
            List<Candle> history = candleService.historyHourly(chatId, p.getSymbol(), 120);
            TpSlOptimizer.Result r = optimizer.optimize(history);
            p.setTakeProfitPct(r.getTpPct());
            p.setStopLossPct(r.getSlPct());
            p.setActive(r.getProfit() > 0);
            pairRepo.save(p);
        }

        String modelPath = String.format("models/%d/ml_signal_filter.onnx", chatId);
        modelTrainer.trainAndExport(chatId, modelPath);
        log.info("✅ Модель обучена для chatId={}, сохранена в {}", chatId, modelPath);
    }
}
