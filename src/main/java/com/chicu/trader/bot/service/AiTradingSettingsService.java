// src/main/java/com/chicu/trader/bot/service/AiTradingSettingsService.java
package com.chicu.trader.bot.service;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.bot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTradingSettingsService {

    private final AiTradingSettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final AiTradingDefaults defaults;
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Получить существующие настройки или создать новые с дефолтами.
     */
    @Transactional
    public AiTradingSettings getOrCreate(Long chatId) {
        return settingsRepo.findById(chatId)
                .orElseGet(() -> {
                    User user = userRepo.findById(chatId)
                            .orElseThrow(() -> new IllegalStateException("User not found: " + chatId));
                    // собираем JSON tp/sl
                    ObjectNode tpSl = om.createObjectNode()
                            .put("tp", defaults.getDefaultTp())
                            .put("sl", defaults.getDefaultSl());
                    AiTradingSettings s = AiTradingSettings.builder()
                            .user(user)
                            .networkMode(defaults.getNetworkMode())
                            .tpSlConfig(tpSl.toString())
                            .reinvestEnabled(defaults.isDefaultReinvest())
                            .symbols("")                          // пусто по-умолчанию
                            .topN(defaults.getDefaultTopN())
                            .riskThreshold(null)
                            .maxDrawdown(null)
                            .timeframe("HOURLY")
                            .commission(null)
                            .build();
                    return settingsRepo.save(s);
                });
    }

    public void updateTpSl(Long chatId, String newTpSlJson) {
        AiTradingSettings s = getOrCreate(chatId);
        s.setTpSlConfig(newTpSlJson);
        settingsRepo.save(s);
    }
    public void resetTpSlDefaults(Long chatId) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("tp", defaults.getDefaultTp());
        node.put("sl", defaults.getDefaultSl());
        updateTpSl(chatId, node.toString());
    }

    // … остальные методы для обновления networkMode, reinvestEnabled и т.д.
}
