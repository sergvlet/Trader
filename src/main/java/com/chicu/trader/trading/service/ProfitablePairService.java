package com.chicu.trader.trading.service;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.repository.ProfitablePairRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitablePairService {

    private final ProfitablePairRepository pairRepository;
    private final AiTradingSettingsService settingsService;

    /**
     * Все сохранённые для пользователя пары.
     */
    public List<ProfitablePair> getAllPairs(Long chatId) {
        return pairRepository.findByUserChatId(chatId);
    }

    /**
     * Только активные пары, отмеченные пользователем.
     */
    public List<ProfitablePair> getActivePairs(Long chatId) {
        return pairRepository.findByUserChatIdAndActiveTrue(chatId);
    }

    /**
     * Получить все пары пользователя по конкретному символу.
     */
    public List<ProfitablePair> getPairsBySymbol(Long chatId, String symbol) {
        return pairRepository.findByUserChatIdAndSymbol(chatId, symbol);
    }

    /**
     * Сохраняет или обновляет пару.
     */
    @Transactional
    public void saveOrUpdate(ProfitablePair pair) {
        List<ProfitablePair> existing = pairRepository.findByUserChatIdAndSymbol(
                pair.getUserChatId(), pair.getSymbol()
        );

        if (existing.isEmpty()) {
            pairRepository.save(pair);
        } else {
            existing.forEach(p -> {
                p.setTakeProfitPct(pair.getTakeProfitPct());
                p.setStopLossPct(pair.getStopLossPct());
                p.setActive(pair.getActive());
            });
            pairRepository.saveAll(existing);
        }
    }

    /**
     * Включает/отключает пару.
     * Если нет записи — создаёт новую с TP/SL из AiTradingSettings.
     * Если есть — переключает флаг активности.
     */
    @Transactional
    public void togglePair(Long chatId, String symbol) {
        List<ProfitablePair> existing = pairRepository.findByUserChatIdAndSymbol(chatId, symbol);

        if (existing.isEmpty()) {
            AiTradingSettings settings = settingsService.getOrCreate(chatId);
            double tp = Optional.ofNullable(settings.getRiskThreshold()).orElse(2.0);
            double sl = Optional.ofNullable(settings.getMaxDrawdown()).orElse(1.0);

            ProfitablePair pair = ProfitablePair.builder()
                    .userChatId(chatId)
                    .symbol(symbol)
                    .takeProfitPct(tp)
                    .stopLossPct(sl)
                    .active(true)
                    .build();

            log.info("Создана новая активная пара {} для chatId={}", symbol, chatId);
            pairRepository.save(pair);
        } else {
            boolean anyActive = existing.stream().anyMatch(ProfitablePair::getActive);
            boolean newState = !anyActive;

            existing.forEach(p -> p.setActive(newState));
            pairRepository.saveAll(existing);
            log.info("Пары {} для chatId={} переключены на active={}", symbol, chatId, newState);
        }
    }
}
