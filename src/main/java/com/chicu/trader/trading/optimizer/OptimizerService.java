package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.repository.ProfitablePairRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizerService {

    private final ProfitablePairRepository pairRepository;
    private final TpSlOptimizer tpSlOptimizer;  // Внедри сюда через DI

    /**
     * Оптимизация TP/SL для всех пар пользователя.
     */
    public void optimizeAllForChat(Long chatId) {
        List<ProfitablePair> pairs = pairRepository.findByUserChatId(chatId);
        for (ProfitablePair pair : pairs) {
            optimizeSingle(pair);
        }
    }

    private void optimizeSingle(ProfitablePair pair) {
        TpSlOptimizer.Result opt = tpSlOptimizer.optimize(pair.getUserChatId());

        pair.setTakeProfitPct(opt.getTpPct() * 100.0);  // конвертация в проценты
        pair.setStopLossPct(opt.getSlPct() * 100.0);

        pairRepository.save(pair);
        log.info("Оптимизирована пара {} → TP={}%, SL={}%", pair.getSymbol(), opt.getTpPct() * 100.0, opt.getSlPct() * 100.0);
    }
}
