package com.chicu.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CallbackCommand {
    /**
     * Ключ (callbackData), на который этот Command срабатывает
     */
    String getKey();

    /**
     * Обработать Update (callbackQuery). 
     * @param update   входящий Update от Telegram
     * @param sender   бот-объект для отправки сообщений
     */
    void execute(Update update, AbsSender sender);
}
