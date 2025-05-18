package com.chicu.trader.config;

import com.chicu.trader.bot.TraderTelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotInitializer {

    /**
     * Регистрируем наш бот в TelegramBotsApi.
     * DefaultBotSession берёт на себя запуск WebSocket/LongPolling.
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TraderTelegramBot traderTelegramBot) throws Exception {
        // создаём API, указывая DefaultBotSession
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        // регистрируем ваш бот
        botsApi.registerBot(traderTelegramBot);
        return botsApi;
    }
}
