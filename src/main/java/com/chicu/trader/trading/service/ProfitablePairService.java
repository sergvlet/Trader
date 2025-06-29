package com.chicu.trader.trading.service;

import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.repository.ProfitablePairRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfitablePairService {

    private final ProfitablePairRepository pairRepository;

    /**
     * Стоп-лосс по умолчанию при создании новой пары.
     * Замените на свои дефолтные значения или забирайте из настроек пользователя.
     */
    private static final double DEFAULT_STOP_LOSS = 0.01;

    /**
     * Тейк-профит по умолчанию при создании новой пары.
     */
    private static final double DEFAULT_TAKE_PROFIT = 0.02;

    /**
     * Все сохранённые для пользователя пары.
     */
    public List<ProfitablePair> getAllPairs(Long chatId) {
        return pairRepository.findByUserChatId(chatId);
    }

    /**
     * Активные (отмеченные) пары для пользователя.
     */
    public List<ProfitablePair> getActivePairs(Long chatId) {
        return pairRepository.findByUserChatIdAndActiveTrue(chatId);
    }

    /**
     * Переключает состояние доступности пары:
     * - если для chatId+symbol записей нет → создаёт новую активную пару
     *   с дефолтными TP/SL;
     * - иначе, если есть хоть одна активная запись → деактивирует ВСЕ;
     *   иначе (все неактивны) → активирует ВСЕ.
     */
    @Transactional
    public void togglePair(Long chatId, String symbol) {
        List<ProfitablePair> pairs = pairRepository.findByUserChatIdAndSymbol(chatId, symbol);

        if (pairs.isEmpty()) {
            // Ни одной записи нет → создаём новую активную пару
            ProfitablePair newPair = ProfitablePair.builder()
                    .userChatId(chatId)
                    .symbol(symbol)
                    .stopLossPct(DEFAULT_STOP_LOSS)
                    .takeProfitPct(DEFAULT_TAKE_PROFIT)
                    .active(true)
                    .build();
            pairRepository.save(newPair);
        } else {
            // Проверяем, есть ли среди них активные
            boolean anyActive = pairs.stream().anyMatch(ProfitablePair::getActive);
            // Переключаем всем сразу: если хоть одна была активна → деактивируем всех, иначе активируем всех
            pairs.forEach(p -> p.setActive(!anyActive));
            pairRepository.saveAll(pairs);
        }
    }
    public List<ProfitablePair> getPairsBySymbol(Long chatId, String symbol) {
        return pairRepository.findByUserChatIdAndSymbol(chatId, symbol);
    }

    /**
     * Сохраняет или обновляет пару.
     */
    public void saveOrUpdate(ProfitablePair pair) {
        pairRepository.save(pair);
    }
}
