// src/main/java/com/chicu/trader/bot/TraderTelegramBot.java
package com.chicu.trader.bot;

import com.chicu.trader.bot.command.CallbackCommand;
import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.config.TelegramBotProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraderTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperties props;
    private final MenuService menuService;
    private final ApplicationEventPublisher publisher;
    private final List<CallbackCommand> callbackCommands;

    private Map<String, CallbackCommand> callbackCommandMap;

    @PostConstruct
    private void init() {
        callbackCommandMap = callbackCommands.stream()
            .collect(Collectors.toUnmodifiableMap(CallbackCommand::getKey, c -> c));
    }

    @Override public String getBotUsername() { return props.getUsername(); }
    @Override public String getBotToken()    { return props.getToken(); }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = extractChatId(update);

        menuService.popNotice(chatId).ifPresent(this::executeUnchecked);

        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();

            if ("ai_trading:start".equals(data)) {
                data = "ai_trading";
            } else if ("ai_trading:stop".equals(data)) {
                data = "ai_trading";
            } else {
                CallbackCommand cmd = callbackCommandMap.get(data);
                if (cmd != null) {
                    cmd.execute(update, this);
                }
                data = menuService.handleInput(update);
            }

            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            SendMessage rendered = menuService.renderState(data, chatId);
            EditMessageText edit = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(rendered.getText())
                .parseMode(rendered.getParseMode())
                .replyMarkup((InlineKeyboardMarkup) rendered.getReplyMarkup())
                .build();
            executeUnchecked(edit);

        } else {
            String next = menuService.handleInput(update);
            SendMessage out = menuService.renderState(next, chatId);
            executeUnchecked(out);
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
                return; // игнорируем
            }
            log.error("Telegram API request failed: {}", resp, e);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            log.error("Telegram API error on {}: {}", method, e.getMessage(), e);
        }
    }

}
