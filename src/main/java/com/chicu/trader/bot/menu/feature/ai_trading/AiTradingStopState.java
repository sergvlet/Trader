// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingStopState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component("ai_trading:stop")
@RequiredArgsConstructor
public class AiTradingStopState implements MenuState {

    private final ApplicationEventPublisher publisher;
    private final AiTradingState aiTradingState;

    @Override
    public String name() {
        return "ai_trading:stop";
    }

    @Override
    public SendMessage render(Long chatId) {
        return aiTradingState.render(chatId);
    }

    @Override
    public String handleInput(org.telegram.telegrambots.meta.api.objects.Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        return "ai_trading";
    }
}
