// src/main/java/com/chicu/trader/bot/command/CallbackCommand.java
package com.chicu.trader.bot.command;


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CallbackCommand {
    /** ключ совпадает с callbackData кнопки, например "register" */
    String getKey();

    /** выполняем логику (редактируем текущее сообщение или шлём новое) */
    void execute(Update update, AbsSender sender);
}
