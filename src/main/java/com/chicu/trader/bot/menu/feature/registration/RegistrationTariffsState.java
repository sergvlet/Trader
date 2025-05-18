// src/main/java/com/chicu/trader/bot/menu/feature/registration/RegistrationTariffsState.java
package com.chicu.trader.bot.menu.feature.registration;

import com.chicu.trader.bot.entity.User;
import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class RegistrationTariffsState implements MenuState {

    private final UserRepository userRepo;
    private final InlineKeyboardMarkup keyboard;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public RegistrationTariffsState(UserRepository userRepo) {
        this.userRepo = userRepo;

        InlineKeyboardButton trial2w = InlineKeyboardButton.builder()
            .text("🎁 Пробный 2 недели")
            .callbackData("register_plan:trial_2weeks")
            .build();
        InlineKeyboardButton m1 = InlineKeyboardButton.builder()
            .text("1 месяц")
            .callbackData("register_plan:1month")
            .build();
        InlineKeyboardButton m3 = InlineKeyboardButton.builder()
            .text("3 месяца")
            .callbackData("register_plan:3months")
            .build();
        InlineKeyboardButton m6 = InlineKeyboardButton.builder()
            .text("6 месяцев")
            .callbackData("register_plan:6months")
            .build();
        InlineKeyboardButton y1 = InlineKeyboardButton.builder()
            .text("1 год")
            .callbackData("register_plan:1year")
            .build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
            .text("‹ Назад")
            .callbackData(MenuService.BACK)
            .build();

        this.keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(trial2w, m1),
                List.of(m3, m6),
                List.of(y1),
                List.of(back)
            ))
            .build();
    }

    @Override
    public String name() {
        return "registration_tariffs";
    }

    @Override
    public SendMessage render(Long chatId) {
        // Получаем дату регистрации и форматируем
        User user = userRepo.findById(chatId).orElseThrow();
        LocalDateTime regDateTime = user.getRegistrationDate();
        String regDate = regDateTime.format(dtf);

        String text = "*Тарифы*  \n" +
                      "Дата регистрации: " + regDate + "  \n\n" +
                      "Выберите режим подписки:";
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .parseMode("Markdown")
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public String handleInput(Update update) {
        if (!update.hasCallbackQuery()) {
            return name();
        }
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("register_plan:")) {
            String plan = data.substring("register_plan:".length());
            log.info("Пользователь {} выбрал тариф {}", chatId, plan);
            // Здесь можно сохранить выбранный план в БД и/или инициировать оплату
            return MenuService.BACK;
        }
        if (MenuService.BACK.equals(data)) {
            return MenuService.BACK;
        }
        return name();
    }
}
