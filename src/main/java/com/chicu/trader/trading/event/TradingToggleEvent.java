// src/main/java/com/chicu/trader/trading/event/TradingToggleEvent.java
package com.chicu.trader.trading.event;

import org.springframework.context.ApplicationEvent;

public class TradingToggleEvent extends ApplicationEvent {
    private final Long chatId;
    private final boolean start;

    public TradingToggleEvent(Long chatId, boolean start) {
        super(chatId);
        this.chatId = chatId;
        this.start = start;
    }

    public Long getChatId() {
        return chatId;
    }

    public boolean isStart() {
        return start;
    }
}
