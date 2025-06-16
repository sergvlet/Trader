package com.chicu.trader.trading.ml;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.trading.entity.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultMlModelTrainer implements MlModelTrainer {

    private final DataLoader dataLoader;
    private final ModelTrainerInternal internalTrainer;
    private final AiTradingSettingsRepository settingsRepository;

    @Override
    public MlTrainingMetrics trainAndExport(Long chatId, String modelPath) throws MlTrainingException {
        // 1) Загрузить настройки пользователя
        AiTradingSettings settings = settingsRepository.findById(chatId)
                .orElseThrow(() -> new MlTrainingException("Настройки не найдены для chatId=" + chatId));

        // 2) Разобрать первую пару из CSV symbols
        String[] symbols = settings.getSymbols().split(",");
        if (symbols.length == 0) {
            throw new MlTrainingException("Пустой список symbols в настройках для chatId=" + chatId);
        }
        String symbol    = symbols[0].trim();
        String timeframe = settings.getTimeframe();

        // 3) Загрузить свечи из БД
        List<Candle> candles = dataLoader.loadCandles(symbol, timeframe);

        // 4) Подготовить Dataset из списка свечей
        Dataset dataset = DatasetBuilder.from(candles);

        // 5) Обучить модель
        Model model = internalTrainer.train(dataset);

        // 6) Экспортировать её в ONNX
        OnnxExporter.export(model, modelPath);

        // 7) Сформировать метрики (пока заглушки)
        MlTrainingMetrics metrics = MlTrainingMetrics.builder()
                .accuracy(0.0)           // TODO: возьмите реальные model.getAccuracy() и т.п.
                .auc(0.0)                
                .precision(0.0)          
                .recall(0.0)             
                .trainingTimeMillis(0L)  
                .build();

        return metrics;
    }
}
