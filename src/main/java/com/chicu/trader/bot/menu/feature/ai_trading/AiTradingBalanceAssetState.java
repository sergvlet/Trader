package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.menu.util.MenuUtils;
import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTradingBalanceAssetState implements MenuState {

    private final UserSettingsService userSettingsService;
    private final BinanceRestClientFactory restClientFactory;

    @Override
    public String name() {
        return "ai_trading_balance_asset";
    }

    @Override
    public SendMessage render(Long chatId) {
        try {
            var assetOpt = MenuUtils.getSelectedAsset(chatId);
            if (assetOpt.isEmpty()) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("⚠️ Ошибка: монета не указана")
                        .replyMarkup(MenuUtils.backKeyboard("ai_trading_settings_balance"))
                        .build();
            }

            String asset = assetOpt.get();
            boolean isTest = userSettingsService.isTestnet(chatId);
            var creds = userSettingsService.getApiCredentials(chatId);
            var client = restClientFactory.create(creds.getApiKey(), creds.getSecretKey(), isTest);

            var info = client.getFullBalance().get(asset);
            if (info == null) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("⚠️ Монета %s не найдена на аккаунте".formatted(asset))
                        .replyMarkup(MenuUtils.backKeyboard("ai_trading_settings_balance"))
                        .build();
            }

            var text = """
                    💰 *Баланс %s* (%s режим):
                    
                    • Доступно: `%.8f`
                    • В ордерах: `%.8f`
                    • Всего: `%.8f`
                    """.formatted(
                            asset,
                            isTest ? "тестовый" : "реальный",
                            info.getFree().setScale(8, RoundingMode.DOWN),
                            info.getLocked().setScale(8, RoundingMode.DOWN),
                            info.getTotal().setScale(8, RoundingMode.DOWN)
                    );

            InlineKeyboardMarkup kb = MenuUtils.backKeyboard("ai_trading_settings_balance");

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .replyMarkup(kb)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при отображении монеты", e);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("❌ Не удалось загрузить баланс монеты: " + e.getMessage())
                    .replyMarkup(MenuUtils.backKeyboard("ai_trading_settings_balance"))
                    .build();
        }
    }

    @Override
    public String handleInput(Update update) {
        return "ai_trading_settings_balance";
    }
}
