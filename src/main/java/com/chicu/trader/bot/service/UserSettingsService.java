package com.chicu.trader.bot.service;

import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserRepository;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "userSettings")
public class UserSettingsService {

    private final UserSettingsRepository settingsRepo;
    private final UserRepository         userRepo;

    /**
     * Возвращает настройки пользователя (JOIN users).
     * Кэшируется на 5 минут (см. конфиг CacheManager).
     */
    @Cacheable(key = "#chatId")
    public UserSettings getSettings(Long chatId) {
        return settingsRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found for chatId=" + chatId));
    }

    /**
     * Меняет биржу и сбрасывает кэш настроек.
     */
    @Transactional
    @CacheEvict(key = "#chatId")
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

    /**
     * Возвращает текущую биржу без обращения к БД (из кэша).
     */
    public String getExchange(Long chatId) {
        return getSettings(chatId).getExchange();
    }

    /**
     * Меняет режим (REAL/TESTNET) и сбрасывает кэш настроек.
     */
    @Transactional
    @CacheEvict(key = "#chatId")
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

    /**
     * Возвращает текущий режим без обращения к БД (из кэша).
     */
    public String getMode(Long chatId) {
        return Optional.ofNullable(getSettings(chatId).getMode()).orElse("REAL");
    }

    /**
     * Устанавливает API-ключ и сбрасывает кэш настроек.
     */
    @Transactional
    @CacheEvict(key = "#chatId")
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
        UserSettings s = getSettings(chatId);
        if (isTestnet(chatId)) {
            settingsRepo.updateTestApiKey(chatId, apiKey);
        } else {
            settingsRepo.updateRealApiKey(chatId, apiKey);
        }
    }

    /**
     * Устанавливает секретный ключ и сбрасывает кэш настроек.
     */
    @Transactional
    @CacheEvict(key = "#chatId")
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
        UserSettings s = getSettings(chatId);
        if (isTestnet(chatId)) {
            settingsRepo.updateTestSecretKey(chatId, secretKey);
        } else {
            settingsRepo.updateRealSecretKey(chatId, secretKey);
        }
    }

    /**
     * Проверяет, что для текущего режима заданы оба ключа.
     * Не обращается к БД (читается из кэша).
     */
    public boolean hasCredentials(Long chatId) {
        UserSettings s = getSettings(chatId);
        String mode = Optional.ofNullable(s.getMode()).orElse("REAL");
        if (mode.toUpperCase().startsWith("TEST")) {
            return Objects.nonNull(s.getTestApiKey()) && Objects.nonNull(s.getTestSecretKey());
        } else {
            return Objects.nonNull(s.getRealApiKey()) && Objects.nonNull(s.getRealSecretKey());
        }
    }

    /**
     * Тестовое соединение — пока просто проверяет наличие ключей.
     */
    public boolean testConnection(Long chatId) {
        return hasCredentials(chatId);
    }

    /**
     * Возвращает ApiCredentials для текущего режима без повторных запросов к БД.
     */
    public ApiCredentials getApiCredentials(Long chatId) {
        UserSettings s = getSettings(chatId);
        String mode = Optional.ofNullable(s.getMode()).orElse("REAL");
        if (mode.toUpperCase().startsWith("TEST")) {
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
     * Возвращает true, если пользователь в режиме TESTNET, без лишних запросов к БД.
     */
    public boolean isTestnet(Long chatId) {
        String mode = Optional.ofNullable(getSettings(chatId).getMode()).orElse("REAL");
        return mode.toUpperCase().startsWith("TEST");
    }
    public UserSettings getOrThrow(Long chatId) {
        return settingsRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found for chatId=" + chatId));
    }
}
