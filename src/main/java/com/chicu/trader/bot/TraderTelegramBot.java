// src/main/java/com/chicu/trader/bot/TraderTelegramBot.java
package com.chicu.trader.bot;

import com.chicu.trader.bot.command.CallbackCommand;
import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.config.TelegramBotProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraderTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperties props;
    private final MenuService menuService;
    private final List<CallbackCommand> callbackCommands;

    private Map<String, CallbackCommand> callbackCommandMap;

    @PostConstruct
    private void init() {
        callbackCommandMap = callbackCommands.stream()
                .collect(Collectors.toUnmodifiableMap(CallbackCommand::getKey, c -> c));
    }

    @Override public String getBotUsername() { return props.getUsername(); }
    @Override public String getBotToken()    { return props.getToken();   }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId;
        try {
            chatId = extractChatId(update);
        } catch (Exception e) {
            log.error("Не удалось извлечь chatId из обновления", e);
            return;
        }

        // 1) вывести накопленные нотификации
        try {
            menuService.popNotice(chatId).ifPresent(this::executeUnchecked);
        } catch (Exception e) {
            log.error("Ошибка popNotice для chatId={}", chatId, e);
        }

        // 2) дальше в зависимости от типа входа
        if (update.hasCallbackQuery()) {
            handleCallback(update, chatId);
        } else {
            handleMessage(update, chatId);
        }
    }

    private void handleCallback(Update update, Long chatId) {
        String data = update.getCallbackQuery().getData();

        // a) CallbackCommand
        try {
            CallbackCommand cmd = callbackCommandMap.get(data);
            if (cmd != null) {
                cmd.execute(update, this);
            }
        } catch (Exception e) {
            log.error("Ошибка в CallbackCommand для data={}", data, e);
        }

        // b) state switch
        String nextState;
        try {
            nextState = menuService.handleInput(update);
        } catch (Exception e) {
            log.error("Ошибка handleInput (callback) data={}", data, e);
            nextState = "main";  // <-- имя вашего корневого состояния
        }

        // c) render & edit
        try {
            SendMessage rendered = menuService.renderState(nextState, chatId);
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(update.getCallbackQuery().getMessage().getMessageId())
                    .text(rendered.getText())
                    .parseMode(rendered.getParseMode())
                    .replyMarkup((InlineKeyboardMarkup) rendered.getReplyMarkup())
                    .build();
            executeUnchecked(edit);
        } catch (Exception e) {
            log.error("Ошибка renderState/executeUnchecked (callback)", e);
            // на всякий случай сбросить в главное
            executeUnchecked(menuService.renderState("main", chatId));
        }
    }

    private void handleMessage(Update update, Long chatId) {
        String nextState;
        try {
            nextState = menuService.handleInput(update);
        } catch (Exception e) {
            log.error("Ошибка handleInput (message)", e);
            nextState = "main";  // <-- имя вашего корневого состояния
        }

        try {
            SendMessage out = menuService.renderState(nextState, chatId);
            executeUnchecked(out);
        } catch (Exception e) {
            log.error("Ошибка renderState/executeUnchecked (message)", e);
            executeUnchecked(menuService.renderState("main", chatId));
        }
    }

    private Long extractChatId(Update u) {
        if (u.hasCallbackQuery()) {
            return u.getCallbackQuery().getMessage().getChatId();
        } else if (u.hasMessage()) {
            return u.getMessage().getChatId();
        }
        throw new IllegalArgumentException("Cannot extract chatId");
    }

    public void executeUnchecked(BotApiMethod<?> method) {
        try {
            execute(method);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException e) {
            String resp = e.getApiResponse();
            if (resp != null && resp.contains("message is not modified")) {
                // benign
                return;
            }
            log.error("Telegram API request failed: {}", resp, e);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            log.error("Telegram API error on {}: {}", method, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при исполнении метода Telegram API", e);
        }
    }
}
