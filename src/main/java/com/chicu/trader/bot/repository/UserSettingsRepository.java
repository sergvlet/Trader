// src/main/java/com/chicu/trader/bot/repository/UserSettingsRepository.java
package com.chicu.trader.bot.repository;

import com.chicu.trader.bot.entity.UserSettings;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {



    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.exchange = :exchange WHERE u.chatId = :chatId")
    int updateExchange(@Param("chatId") Long chatId, @Param("exchange") String exchange);

    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.mode = :mode WHERE u.chatId = :chatId")
    int updateMode(@Param("chatId") Long chatId, @Param("mode") String mode);

    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.testApiKey = :apiKey WHERE u.chatId = :chatId")
    int updateTestApiKey(@Param("chatId") Long chatId, @Param("apiKey") String apiKey);

    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.testSecretKey = :secretKey WHERE u.chatId = :chatId")
    int updateTestSecretKey(@Param("chatId") Long chatId, @Param("secretKey") String secretKey);

    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.realApiKey = :apiKey WHERE u.chatId = :chatId")
    int updateRealApiKey(@Param("chatId") Long chatId, @Param("apiKey") String apiKey);

    @Modifying
    @Transactional
    @Query("UPDATE UserSettings u SET u.realSecretKey = :secretKey WHERE u.chatId = :chatId")
    int updateRealSecretKey(@Param("chatId") Long chatId, @Param("secretKey") String secretKey);
}
