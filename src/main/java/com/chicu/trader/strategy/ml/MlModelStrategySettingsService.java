package com.chicu.trader.strategy.ml;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Сервис для работы с настройками ML-модели для стратегии ML_MODEL.
 */
@Service
@RequiredArgsConstructor
public class MlModelStrategySettingsService {

    private final MlModelStrategySettingsRepository repo;
    private final AiTradingSettingsRepository aiRepo;

    /**
     * Возвращает настройки, либо создаёт новую запись со значениями по умолчанию.
     */
    @Transactional
    public MlModelStrategySettings getOrCreate(Long chatId) {
        return repo.findById(chatId)
                .orElseGet(() -> createNew(chatId));
    }

    /**
     * Создаёт новую запись с дефолтными параметрами.
     */
    @Transactional
    public MlModelStrategySettings createNew(Long chatId) {
        AiTradingSettings ai = aiRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("AiTradingSettings not found: " + chatId));

        MlModelStrategySettings cfg = new MlModelStrategySettings();
        cfg.setChatId(chatId);
        cfg.setAiTradingSettings(ai);
        cfg.setModelPath("models/ml_model_default.pkl");
        cfg.setFeatureList("close,volume");
        cfg.setThreshold(0.5);
        cfg.setLastTrainedAt(Instant.now());

        return repo.saveAndFlush(cfg);
    }

    /**
     * Сохраняет вручную отредактированные настройки.
     */
    @Transactional
    public void save(MlModelStrategySettings cfg) {
        repo.saveAndFlush(cfg);
    }

    /**
     * Обновляет настройки из Python-обучения (JSON → параметры модели).
     */
    @Transactional
    public void updateFromTrainingResult(Long chatId,
                                         String modelPath,
                                         String featureList,
                                         Double threshold,
                                         Integer nEstimators,
                                         Integer maxDepth,
                                         Double learningRate) {

        MlModelStrategySettings cfg = getOrCreate(chatId);
        cfg.setModelPath(modelPath);
        cfg.setFeatureList(featureList);
        cfg.setThreshold(threshold);
        cfg.setNEstimators(nEstimators);
        cfg.setMaxDepth(maxDepth);
        cfg.setLearningRate(learningRate);
        cfg.setLastTrainedAt(Instant.now());

        repo.saveAndFlush(cfg);
    }
}
