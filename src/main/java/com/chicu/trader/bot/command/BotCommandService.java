package com.chicu.trader.bot.command;

import com.chicu.trader.ml.MlTrainingService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Service
public class BotCommandService {
    private final MlTrainingService trainingService;
    private final AbsSender telegramSender;

    public BotCommandService(MlTrainingService trainingService,
                             AbsSender telegramSender) {
        this.trainingService = trainingService;
        this.telegramSender = telegramSender;
    }

    public void handleTrainCommand(Long chatId) {
        sendText(chatId, "🚀 Запуск сбора данных и обучения ML-модели. Пожалуйста, подождите...");

        boolean success = trainingService.runTraining();
        if (success) {
            sendText(chatId, "✅ Обучение завершилось успешно! Модель обновлена.");
        } else {
            sendText(chatId, "❌ При обучении модели произошла ошибка. Проверьте логи.");
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText(text);
            telegramSender.execute(msg);
        } catch (Exception e) {
            e.printStackTrace(); // по желанию: заменить на log.warn
        }
    }
}
