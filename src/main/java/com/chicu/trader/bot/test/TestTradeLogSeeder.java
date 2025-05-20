package com.chicu.trader.bot.test;


import com.chicu.trader.model.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestTradeLogSeeder {

    private final TradeLogRepository tradeLogRepository;

    public void seedLogs(Long chatId) {
        List<TradeLog> existing = tradeLogRepository.findAllByUserChatId(chatId);
        if (!existing.isEmpty()) {
            log.info("🔁 TradeLog уже существует для chatId={}, не добавляем повторно", chatId);
            return;
        }

        TradeLog t1 = TradeLog.builder()
                .userChatId(chatId)
                .symbol("BTCUSDT")
                .entryPrice(10000.0)
                .exitPrice(10500.0)
                .entryTime(Instant.now().minusSeconds(3600))
                .exitTime(Instant.now().minusSeconds(1800))
                .pnl(500.0)
                .isClosed(true)
                .build();

        TradeLog t2 = TradeLog.builder()
                .userChatId(chatId)
                .symbol("ETHUSDT")
                .entryPrice(2000.0)
                .exitPrice(1950.0)
                .entryTime(Instant.now().minusSeconds(7200))
                .exitTime(Instant.now().minusSeconds(3600))
                .pnl(-50.0)
                .isClosed(true)
                .build();

        TradeLog t3 = TradeLog.builder()
                .userChatId(chatId)
                .symbol("SOLUSDT")
                .entryPrice(25.0)
                .exitPrice(28.0)
                .entryTime(Instant.now().minusSeconds(5400))
                .exitTime(Instant.now().minusSeconds(2700))
                .pnl(3.0)
                .isClosed(true)
                .build();

        tradeLogRepository.saveAll(List.of(t1, t2, t3));
        log.info("✅ Добавлены тестовые записи TradeLog для chatId={}", chatId);
    }
}
