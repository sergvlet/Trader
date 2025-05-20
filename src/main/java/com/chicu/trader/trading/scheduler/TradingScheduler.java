package com.chicu.trader.trading.scheduler;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.StrategyFacade;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TradingScheduler {

    private final ProfitablePairRepository pairRepo;
    private final CandleService            candleService;
    private final StrategyFacade           strategyFacade;

    /**
     * Каждые 5 секунд проверяем для каждого чата и пары последнюю свечу.
     */
    @Scheduled(fixedDelay = 5_000)
    public void tick() {
        List<ProfitablePair> allActive = pairRepo.findAll()
            .stream().filter(ProfitablePair::isActive).toList();

        // сгруппировать по chatId
        allActive.stream()
            .map(p -> Map.entry(p.getUserChatId(), p))
            .collect(Collectors.groupingBy(Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
            .forEach((chatId, pairs) -> {
                for (ProfitablePair p : pairs) {
                    Candle last = candleService.history(p.getSymbol(), Duration.ofMinutes(1), 1).get(0);
                    strategyFacade.applyStrategies(chatId, last, List.of(p));
                }
            });
    }
}
