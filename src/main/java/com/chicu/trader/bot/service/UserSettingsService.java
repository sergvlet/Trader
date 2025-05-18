// src/main/java/com/chicu/trader/bot/service/UserSettingsService.java
package com.chicu.trader.bot.service;

import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserRepository;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository settingsRepo;
    private final UserRepository         userRepo;

    @Transactional
    public void setExchange(Long chatId, String exchange) {
        if (settingsRepo.existsById(chatId)) {
            settingsRepo.updateExchange(chatId, exchange);
        } else {
            User user = userRepo.findById(chatId)
                    .orElseGet(() -> userRepo.save(User.builder()
                            .chatId(chatId)
                            .tradingEnabled(false)
                            .build()));
            UserSettings s = UserSettings.builder()
                    .chatId(chatId)
                    .user(user)
                    .exchange(exchange)
                    .build();
            settingsRepo.save(s);
        }
    }

    public String getExchange(Long chatId) {
        return settingsRepo.findById(chatId)
                .map(UserSettings::getExchange)
                .orElse(null);
    }

    @Transactional
    public void setMode(Long chatId, String mode) {
        if (settingsRepo.existsById(chatId)) {
            settingsRepo.updateMode(chatId, mode);
        } else {
            User user = userRepo.findById(chatId)
                    .orElseGet(() -> userRepo.save(User.builder()
                            .chatId(chatId)
                            .tradingEnabled(false)
                            .build()));
            UserSettings s = UserSettings.builder()
                    .chatId(chatId)
                    .user(user)
                    .mode(mode)
                    .build();
            settingsRepo.save(s);
        }
    }

    public String getMode(Long chatId) {
        return settingsRepo.findById(chatId)
                .map(UserSettings::getMode)
                .orElse(null);
    }

    @Transactional
    public void setApiKey(Long chatId, String apiKey) {
        if (!settingsRepo.existsById(chatId)) {
            User user = userRepo.findById(chatId)
                    .orElseGet(() -> userRepo.save(User.builder()
                            .chatId(chatId)
                            .tradingEnabled(false)
                            .build()));
            settingsRepo.save(UserSettings.builder()
                    .chatId(chatId)
                    .user(user)
                    .build());
        }
        settingsRepo.findById(chatId).ifPresent(s -> {
            if ("REAL".equalsIgnoreCase(s.getMode())) {
                settingsRepo.updateRealApiKey(chatId, apiKey);
            } else {
                settingsRepo.updateTestApiKey(chatId, apiKey);
            }
        });
    }

    @Transactional
    public void setSecretKey(Long chatId, String secretKey) {
        if (!settingsRepo.existsById(chatId)) {
            User user = userRepo.findById(chatId)
                    .orElseGet(() -> userRepo.save(User.builder()
                            .chatId(chatId)
                            .tradingEnabled(false)
                            .build()));
            settingsRepo.save(UserSettings.builder()
                    .chatId(chatId)
                    .user(user)
                    .build());
        }
        settingsRepo.findById(chatId).ifPresent(s -> {
            if ("REAL".equalsIgnoreCase(s.getMode())) {
                settingsRepo.updateRealSecretKey(chatId, secretKey);
            } else {
                settingsRepo.updateTestSecretKey(chatId, secretKey);
            }
        });
    }

    public boolean hasCredentials(Long chatId) {
        return settingsRepo.findById(chatId)
                .map(s -> s.hasCredentialsFor(s.getMode()))
                .orElse(false);
    }

    /**
     * Проверяет, что для текущего режима у пользователя заданы оба ключа,
     * и при необходимости делает реальное подключение к API.
     */
    public boolean testConnection(Long chatId) {
        return hasCredentials(chatId);
    }

    // === Добавленные методы: ApiCredentials и режим Testnet/Real ===



    /**
     * Возвращает ApiCredentials (apiKey + secretKey) для текущего режима пользователя.
     */
    public ApiCredentials getApiCredentials(Long chatId) {
        UserSettings s = settingsRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("No settings for chatId=" + chatId));
        String mode = Optional.ofNullable(s.getMode()).orElse("REAL");
        if ("TESTNET".equalsIgnoreCase(mode)) {
            if (Objects.isNull(s.getTestApiKey()) || Objects.isNull(s.getTestSecretKey())) {
                throw new IllegalStateException("Missing TESTNET credentials");
            }
            return new ApiCredentials(s.getTestApiKey(), s.getTestSecretKey());
        } else {
            if (Objects.isNull(s.getRealApiKey()) || Objects.isNull(s.getRealSecretKey())) {
                throw new IllegalStateException("Missing REAL credentials");
            }
            return new ApiCredentials(s.getRealApiKey(), s.getRealSecretKey());
        }
    }

    /**
     * Возвращает true, если пользователь в режиме TESTNET.
     */
    public boolean isTestnet(Long chatId) {
        return "TESTNET".equalsIgnoreCase(
                settingsRepo.findById(chatId)
                        .map(UserSettings::getMode)
                        .orElse("REAL")
        );
    }
}
