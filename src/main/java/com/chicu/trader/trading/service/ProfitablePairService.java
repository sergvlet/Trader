package com.chicu.trader.trading.service;

import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.repository.ProfitablePairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfitablePairService {

    private final ProfitablePairRepository pairRepository;

    public List<ProfitablePair> getAllPairs(Long chatId) {
        return pairRepository.findByUserChatId(chatId);
    }

    public List<ProfitablePair> getActivePairs(Long chatId) {
        return pairRepository.findByUserChatIdAndActiveTrue(chatId);
    }

    public void togglePair(Long chatId, String symbol) {
        Optional<ProfitablePair> pairOpt = pairRepository.findByUserChatIdAndSymbol(chatId, symbol);
        if (pairOpt.isPresent()) {
            ProfitablePair pair = pairOpt.get();
            pair.setActive(!pair.getActive());
            pairRepository.save(pair);
        }
    }
    public void saveOrUpdate(ProfitablePair pair) {
        pairRepository.save(pair);
    }
}
