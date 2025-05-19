// src/main/java/com/chicu/trader/bot/menu/feature/ai_trading/AiTradingStartState.java
package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Нажатие ▶️ Начать торговлю
 */
@Component("ai_trading:start")
@RequiredArgsConstructor
public class AiTradingStartState implements MenuState {

    private final ApplicationEventPublisher publisher;
    private final AiTradingState aiTradingState;

    @Override
    public String name() {
        return "ai_trading:start";
    }

    @Override
    public SendMessage render(Long chatId) {
        // Просто возвращаем отрисовку главного меню
        return aiTradingState.render(chatId);
    }

    @Override
    public String handleInput(org.telegram.telegrambots.meta.api.objects.Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        // Сначала публикуем событие старта
        // Затем переходим в основное состояние, чтобы MenuService перерисовал меню
        return "ai_trading";
    }
}
