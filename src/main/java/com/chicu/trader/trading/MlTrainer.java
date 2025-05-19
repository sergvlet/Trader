package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.ml.MlModelTrainer;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.optimizer.TpSlOptimizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MlTrainer {

    private final CandleService candleService;
    private final ProfitablePairRepository pairRepo;
    private final TpSlOptimizer optimizer;
    private final MlModelTrainer modelTrainer;

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
        modelTrainer.trainAndExport("models/ml_signal_filter.onnx");
    }
}
