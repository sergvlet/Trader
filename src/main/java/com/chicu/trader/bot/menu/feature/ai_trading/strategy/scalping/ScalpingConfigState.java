package com.chicu.trader.bot.menu.feature.ai_trading.strategy.scalping;

import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;
import com.chicu.trader.strategy.scalping.service.ScalpingStrategySettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component("scalping_config")
@RequiredArgsConstructor
public class ScalpingConfigState implements MenuState {

    private final ScalpingStrategySettingsService settingsService;

    @Override
    public String name() {
        return "scalping_config";
    }

    @Override
    public SendMessage render(Long chatId) {
        ScalpingStrategySettings cfg = settingsService.getOrCreate(chatId);

        String symbol = cfg.getSymbol();
        String symbolDisplay = (symbol == null || symbol.isBlank())
                ? "— не задано (введите одну пару или через запятую список) —"
                : symbol;

        String text = String.format("""
        *⚡️ Настройки стратегии Scalping*

        • Размер окна (`window size`): `%d` свечей  
        • Порог изменения цены (`Δ%%`): `%.2f%%`  
        • Мин. объём (`min volume`): `%.2f`  
        • Порог спреда (`spread %%`): `%.2f%%`  
        • Take Profit (`TP %%`): `%.2f%%`  
        • Stop Loss (`SL %%`): `%.2f%%`  
        • Таймфрейм (`timeframe`): `%s`  
        • Лимит кэша свечей: `%d`  
        • Символы (`symbols`): `%s`

        Выберите, что хотите изменить:""",
                cfg.getWindowSize(),
                cfg.getPriceChangeThreshold(),
                cfg.getMinVolume(),
                cfg.getSpreadThreshold(),
                cfg.getTakeProfitPct(),
                cfg.getStopLossPct(),
                cfg.getTimeframe(),
                cfg.getCachedCandlesLimit(),
                symbolDisplay
        );

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        button("🖼️ Window size", ":window"),
                        button("📈 Price Δ",        ":price")
                ),
                List.of(
                        button("🔊 Min volume",     ":volume"),
                        button("💲 Spread",         ":spread")
                ),
                List.of(
                        button("🎯 Take profit",    ":tp"),
                        button("🛑 Stop loss",      ":sl")
                ),
                List.of(
                        button("⏱ Таймфрейм",       ":timeframe"),
                        button("🔣 Символы",        ":symbols")
                ),
                List.of(
                        button("🕯 Кэш свечей",      ":candles")
                ),
                List.of(
                        InlineKeyboardButton.builder()
                                .text("‹ Назад")
                                .callbackData("ai_trading_settings_strategy")
                                .build()
                )
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    @Override
    public @NonNull String handleInput(Update upd) {
        if (!upd.hasCallbackQuery()) {
            return name();
        }

        String data = upd.getCallbackQuery().getData();

        if ("ai_trading_settings_strategy".equals(data)) {
            return "ai_trading_settings_strategy";
        }

        return switch (data) {
            case "scalping_config:window"     -> "scalping_set_window";
            case "scalping_config:price"      -> "scalping_set_price";
            case "scalping_config:volume"     -> "scalping_set_volume";
            case "scalping_config:spread"     -> "scalping_set_spread";
            case "scalping_config:tp"         -> "scalping_set_tp";
            case "scalping_config:sl"         -> "scalping_set_sl";
            case "scalping_config:timeframe"  -> "scalping_set_timeframe";
            case "scalping_config:symbols"    -> "scalping_set_symbols";
            case "scalping_config:candles"    -> "scalping_set_candles";
            default                           -> name();
        };
    }

    private InlineKeyboardButton button(String text, String suffix) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(name() + suffix)
                .build();
    }
}
