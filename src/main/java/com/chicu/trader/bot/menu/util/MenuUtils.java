// src/main/java/com/chicu/trader/bot/menu/util/MenuUtils.java
package com.chicu.trader.bot.menu.util;

import com.chicu.trader.bot.menu.core.MenuService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MenuUtils {

    private static MenuService menuService;

    private static final Map<Long, Integer> balancePages = new ConcurrentHashMap<>();
    private static final Map<Long, String> selectedAssets = new ConcurrentHashMap<>();

    @Autowired
    public MenuUtils(MenuService menuService) {
        MenuUtils.menuService = menuService;
    }

    // --- Отложенные уведомления ---
    public static void deferNotice(Long chatId, String text) {
        if (menuService != null) {
            menuService.deferNotice(chatId, text);
        }
    }

    public static void deferRender(Long chatId, SendMessage message) {
        if (menuService != null) {
            menuService.deferRender(chatId, message);
        }
    }

    // --- Кнопки ---
    public static InlineKeyboardMarkup backKeyboard(String callback) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(backButton(callback))))
                .build();
    }

    public static InlineKeyboardButton backButton(String callback) {
        return InlineKeyboardButton.builder()
                .text("‹ Назад")
                .callbackData(callback)
                .build();
    }

    public static SendMessage noticeOnly(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("↩ Нажмите кнопку в меню")
                .build();
    }

    // === ПАГИНАЦИЯ БАЛАНСА ===

    public static int getPage(Long chatId) {
        return balancePages.getOrDefault(chatId, 0);
    }

    public static void incrementPage(Long chatId) {
        balancePages.put(chatId, getPage(chatId) + 1);
    }

    public static void decrementPage(Long chatId) {
        int curr = getPage(chatId);
        if (curr > 0) {
            balancePages.put(chatId, curr - 1);
        }
    }

    public static void resetPage(Long chatId) {
        balancePages.remove(chatId);
    }

    // === Выбор монеты ===

    public static void setSelectedAsset(Long chatId, String asset) {
        selectedAssets.put(chatId, asset);
    }

    public static Optional<String> getSelectedAsset(Long chatId) {
        return Optional.ofNullable(selectedAssets.get(chatId));
    }
}
