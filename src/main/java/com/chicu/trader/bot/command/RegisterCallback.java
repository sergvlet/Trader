// src/main/java/com/chicu/trader/bot/command/RegisterCallback.java
package com.chicu.trader.bot.command;

import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.repository.UserRepository;
import com.chicu.trader.bot.menu.feature.registration.RegistrationTariffsState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RegisterCallback implements CallbackCommand {

    private final UserRepository userRepo;
    private final RegistrationTariffsState tariffsState;

    @Override
    public String getKey() {
        return "register";
    }

    @Override
    public void execute(Update update, AbsSender sender) {
        var cq      = update.getCallbackQuery();
        Long chatId = cq.getMessage().getChatId();
        Integer msgId = cq.getMessage().getMessageId();
        var from    = cq.getFrom();

        // 1) Сохраняем пользователя
        User user = userRepo.findById(chatId)
            .orElseGet(() -> User.builder()
                .chatId(chatId)
                .build()
            );
        user.setFirstName(from.getFirstName());
        user.setLastName(from.getLastName());
        user.setUsername(from.getUserName());
        user.setLanguageCode(from.getLanguageCode());
        user.setRegistrationDate(LocalDateTime.now());
        userRepo.save(user);

        try {
            // 2) Убираем «часики» у callback
            sender.execute(AnswerCallbackQuery.builder()
                .callbackQueryId(cq.getId())
                .build());

            // 3) Берём готовый SendMessage из RegistrationTariffsState
            var tariffMsg = tariffsState.render(chatId);

            // 4) Редактируем текущее сообщение, выводим меню тарифов
            sender.execute(EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(msgId)
                .text(tariffMsg.getText())
                .replyMarkup((InlineKeyboardMarkup) tariffMsg.getReplyMarkup())
                .parseMode(tariffMsg.getParseMode())
                .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
