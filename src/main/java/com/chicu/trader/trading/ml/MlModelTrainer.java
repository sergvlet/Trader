package com.chicu.trader.trading.ml;

/**
 * Интерфейс тренировки ML‐модели.
 */
public interface MlModelTrainer {
    /**
     * Обучить модель для chatId и сохранить артефакт по пути modelPath.
     *
     * @param chatId    идентификатор пользователя/бота, для которого обучаем
     * @param modelPath путь (в файловой системе), куда надо сохранить готовую модель
     * @return простейшая структура метрик обучения (точность, auc, время и т.д.)
     * @throws MlTrainingException если что‐то пошло не так при обучении
     */
    MlTrainingMetrics trainAndExport(Long chatId, String modelPath) throws MlTrainingException;
}
