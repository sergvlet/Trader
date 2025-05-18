package com.chicu.trader.bot.service;// в конце файла src/main/java/com/chicu/trader/bot/service/UserSettingsService.java

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
     * DTO с ключами для API
     */
    @Getter
    @AllArgsConstructor
    public class ApiCredentials {
        private final String apiKey;
        private final String secretKey;
    }
