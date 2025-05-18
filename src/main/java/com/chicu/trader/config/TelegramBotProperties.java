package com.chicu.trader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Бин для свойств Telegram-бота, регистрируется через @EnableConfigurationProperties
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {
    private String username;
    private String token;

}
