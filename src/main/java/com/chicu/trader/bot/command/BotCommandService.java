package com.chicu.trader.bot.command;

import com.chicu.trader.ml.PythonTrainingService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Service
public class BotCommandService {
    private final PythonTrainingService pythonTrainingService;
    private final AbsSender telegramSender; // предположим, что у вас есть бин для отправки сообщений

    public BotCommandService(PythonTrainingService pythonTrainingService,
                             AbsSender telegramSender) {
        this.pythonTrainingService = pythonTrainingService;
        this.telegramSender = telegramSender;
    }

    public void handleTrainCommand(Long chatId) {
        // Отправим сразу сообщение, что сбор/обучение запущены
        sendText(chatId, "🚀 Запуск сбора данных и обучения ML-модели. Пожалуйста, подождите...");

        boolean success = pythonTrainingService.runTraining();
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
            e.printStackTrace();
        }
    }
}
