package com.chicu.trader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {
    /**
     * Имя вашего бота, без @
     */
    private String username;
    /**
     * Токен, который вам дал BotFather
     */
    private String token;

}
