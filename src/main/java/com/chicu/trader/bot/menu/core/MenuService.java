package com.chicu.trader.bot.menu.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MenuService {

    public static final String BACK = "BACK";
    public static final String MAIN_MENU = "main";  // должно существовать состояние с name() = "main"
    public static final String STATE_NETWORK_SETTINGS = "network_settings";

    private final List<MenuState> states;
    private final Map<Long, String> currentStateMap = new ConcurrentHashMap<>();
    private final Map<Long, String> notices = new ConcurrentHashMap<>();

    public MenuService(List<MenuState> states) {
        this.states = states;
    }

    public void deferNotice(Long chatId, String text) {
        notices.put(chatId, text);
    }

    public Optional<SendMessage> popNotice(Long chatId) {
        String txt = notices.remove(chatId);
        if (txt != null) {
            return Optional.of(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(txt)
                    .build());
        }
        return Optional.empty();
    }

    public SendMessage renderState(String stateName, Long chatId) {
        String baseState = baseName(stateName);
        return states.stream()
                .filter(s -> s.name().equals(baseState))
                .findFirst()
                .map(state -> state.render(chatId))
                .orElseGet(() -> {
                    log.warn("State not found in renderState: {}", stateName);
                    return fallbackMessage(chatId);
                });
    }

    public String handleInput(Update update) {
        Long chatId = extractChatId(update);

        String callbackData = update.hasCallbackQuery() ? update.getCallbackQuery().getData() : null;
        String requestedState = callbackData != null ? baseName(callbackData) : null;

        String currentState = currentStateMap.getOrDefault(chatId, MAIN_MENU);
        MenuState state = findState(currentState).orElse(null);

        if (state == null) {
            log.warn("Current state not found: {}, fallback to MAIN_MENU", currentState);
            currentState = MAIN_MENU;
            state = findState(currentState).orElse(null);
        }

        String next = null;
        try {
            if (state != null) {
                next = state.handleInput(update);
            }
        } catch (Exception e) {
            log.error("Ошибка в handleInput для state={}, chatId={}", currentState, chatId, e);
        }

        if (next == null) next = MAIN_MENU;
        if (BACK.equals(next)) next = MAIN_MENU;

        currentStateMap.put(chatId, next);
        return next;
    }

    private Optional<MenuState> findState(String name) {
        return states.stream()
                .filter(s -> s.name().equals(name))
                .findFirst();
    }

    private String baseName(String callbackData) {
        if (callbackData == null) return "";
        return callbackData.contains(":")
                ? callbackData.substring(0, callbackData.indexOf(":"))
                : callbackData;
    }

    private SendMessage fallbackMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("⚠ Неизвестное состояние. Возвращаюсь в главное меню.")
                .build();
    }

    private Long extractChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else {
            throw new IllegalArgumentException("Не могу получить chatId из Update: " + update);
        }
    }
}
