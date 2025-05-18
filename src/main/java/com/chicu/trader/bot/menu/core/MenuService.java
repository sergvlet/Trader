// src/main/java/com/chicu/trader/bot/menu/core/MenuService.java
package com.chicu.trader.bot.menu.core;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MenuService {

    public static final String BACK = "BACK";
    public static final String MAIN_MENU = "MAIN_MENU";
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
        MenuState st = states.stream()
                .filter(s -> s.name().equals(stateName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("State not found: " + stateName));
        return st.render(chatId);
    }

    public String handleInput(Update update) {
        Long chatId = extractChatId(update);
        String curr = currentStateMap.getOrDefault(chatId, MAIN_MENU);
        MenuState state = states.stream()
                .filter(s -> s.name().equals(curr))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown state: " + curr));

        String next = state.handleInput(update);
        if (BACK.equals(next)) {
            next = MAIN_MENU;
        }
        currentStateMap.put(chatId, next);
        return next;
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
