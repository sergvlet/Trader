// src/main/java/com/chicu/trader/trading/TradingStatusService.java
package com.chicu.trader.trading;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradingStatusService {
    private final Set<Long> running = ConcurrentHashMap.newKeySet();
    private final Map<Long, String> lastEvent = new ConcurrentHashMap<>();

    public void markRunning(Long chatId) {
        running.add(chatId);
    }

    public void markStopped(Long chatId) {
        running.remove(chatId);
    }

    public boolean isRunning(Long chatId) {
        return running.contains(chatId);
    }

    public void setLastEvent(Long chatId, String event) {
        lastEvent.put(chatId, event);
    }

    public String getLastEvent(Long chatId) {
        return lastEvent.get(chatId);
    }
}
