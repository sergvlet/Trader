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
import org.telegram.telegrambots.meta.api.methods.stickers.SetStickerSetThumb;
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
    private final List<CallbackCommand> callbackCommands;

    private Map<String, CallbackCommand> callbackCommandMap;

    @PostConstruct
    private void init() {
        callbackCommandMap = callbackCommands.stream()
                .collect(Collectors.toUnmodifiableMap(CallbackCommand::getKey, c -> c));
    }

    @Override
    public String getBotUsername() {
        return props.getUsername();
    }

    @Override
    public String getBotToken() {
        return props.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = extractChatId(update);

        // 1) высылаем отложенное уведомление, если есть
        menuService.popNotice(chatId).ifPresent(this::executeUnchecked);

        // 2) определяем, как обрабатывать этот update
        String nextState = handleCallbackOrMenu(update);

        // 3) рендерим и шлём меню (редактируем для callback-ов, или новое сообщение для текстов)
        if (update.hasCallbackQuery()) {
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            SendMessage rendered = menuService.renderState(nextState, chatId);
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(rendered.getText())
                    .parseMode(rendered.getParseMode())
                    .replyMarkup((InlineKeyboardMarkup) rendered.getReplyMarkup())
                    .build();
            executeUnchecked(edit);
        } else {
            SendMessage out = menuService.renderState(nextState, chatId);
            executeUnchecked(out);
        }
    }

    private String handleCallbackOrMenu(Update update) {
        if (update.hasCallbackQuery()) {
            String key = update.getCallbackQuery().getData();
            CallbackCommand cmd = callbackCommandMap.get(key);
            if (cmd != null) {
                cmd.execute(update, this);
                // всегда возвращаем управление MenuService
                return menuService.handleInput(update);
            }
        }
        return menuService.handleInput(update);
    }

    private Long extractChatId(Update u) {
        if (u.hasCallbackQuery()) {
            return u.getCallbackQuery().getMessage().getChatId();
        } else if (u.hasMessage()) {
            return u.getMessage().getChatId();
        }
        throw new IllegalArgumentException("Не могу достать chatId из Update");
    }

    private void executeUnchecked(BotApiMethod<?> method) {
        try {
            execute(method);
        } catch (TelegramApiException e) {
            log.error("Telegram API error on {}", method, e);
        }
    }
}
