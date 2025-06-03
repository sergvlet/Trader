package com.chicu.trader.ml.service;

import com.chicu.trader.ml.model.MlSignalFilterConfig;
import com.chicu.trader.ml.repository.MlSignalFilterConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MlSignalFilterConfigService {

    private final MlSignalFilterConfigRepository repo;

    public double getThresholdForUser(Long chatId) {
        return repo.findById(chatId)
                .map(MlSignalFilterConfig::getMinChangeThreshold)
                .orElse(0.001); // default
    }

    public void saveOrUpdate(Long chatId, double threshold) {
        MlSignalFilterConfig config = repo.findById(chatId)
                .orElse(new MlSignalFilterConfig(chatId, threshold, null));
        config.setMinChangeThreshold(threshold);
        repo.save(config);
    }
}
