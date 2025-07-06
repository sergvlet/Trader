package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.backtest.BacktestResult;
import com.chicu.trader.trading.backtest.service.BacktestService;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.BacktestSettings;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component("ai_trading_backtesting_config")
public class AiTradingBacktestingConfigState implements MenuState {

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_DATE;

    private final AiTradingSettingsService aiSettingsService;
    private final ProfitablePairService    pairService;
    private final BacktestSettingsService  btService;
    private final BacktestService          backtestService;
    private final StrategyRegistry         strategyRegistry;
    private final MenuService              menuService;

    public AiTradingBacktestingConfigState(
            AiTradingSettingsService aiSettingsService,
            ProfitablePairService pairService,
            BacktestSettingsService btService,
            BacktestService backtestService,
            StrategyRegistry strategyRegistry,
            @Lazy MenuService menuService
    ) {
        this.aiSettingsService = aiSettingsService;
        this.pairService       = pairService;
        this.btService         = btService;
        this.backtestService   = backtestService;
        this.strategyRegistry  = strategyRegistry;
        this.menuService       = menuService;
    }

    @Override
    public String name() {
        return "ai_trading_backtesting_config";
    }

    @Override
    public SendMessage render(Long chatId) {
        // 1) Пользовательские AI-настройки
        AiTradingSettings ai = aiSettingsService.getSettingsOrThrow(chatId);
        String strategyName = String.valueOf(ai.getStrategy());

        // 2) Настройки выбранной стратегии
        StrategySettings stratSettings =
                strategyRegistry.getSettings(StrategyType.valueOf(strategyName), chatId);
        Map<String, Object> stratMap = settingsToMap(stratSettings);
        StringBuilder stratSection = new StringBuilder("*🛠 Настройки стратегии:*\n");
        stratMap.forEach((k, v) ->
                stratSection.append("• ").append(k).append(": `").append(v).append("`\n")
        );
        stratSection.append("\n");

        // 3) Список пар (ручной ввод или активные из БД)
        List<String> symbols = Optional.ofNullable(ai.getSymbols())
                .filter(s -> !s.isBlank())
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim).filter(t -> !t.isEmpty()).toList())
                .orElseGet(() -> pairService.getActivePairs(chatId).stream()
                        .map(ProfitablePair::getSymbol).toList());
        String pairsList = symbols.isEmpty() ? "— не выбраны —" : String.join(", ", symbols);

        // 4) Параметры бэктеста
        BacktestSettings cfg = btService.getOrCreate(chatId);

        var text = new StringBuilder()
                // Стратегия и её настройки
                .append("*✅ Стратегия:* `").append(strategyName).append("`\n\n")
                .append(stratSection)
                // Пары
                .append("*🔀 Пары:* `").append(pairsList).append("`\n\n")
                // Настройки бэктеста
                .append("*⚙️ Настройки бэктеста:*\n")
                .append(String.format("• Период: `%s` → `%s`\n",
                        cfg.getStartDate().format(DF),
                        cfg.getEndDate().format(DF)))
                .append(String.format("• Комиссия: `%.2f%%`\n", cfg.getCommissionPct()))
                .append(String.format("• Таймфрейм: `%s`\n", cfg.getTimeframe()))
                .append(String.format("• Свечей (кэш limit): `%d`\n", cfg.getCachedCandlesLimit()))
                .append(String.format("• Плечо: `%dx`\n\n", cfg.getLeverage()))
                .append("Выберите, что хотите изменить:");

        // 5) Предыдущий результат
        menuService.popNotice(chatId).ifPresent(n ->
                text.append("\n\n").append(n.getText())
        );

        // 6) Кнопки
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(button("📅 Период",     ":period")),
                List.of(button("💰 Комиссия",   ":commission")),
                List.of(button("⏱ Таймфрейм",  ":timeframe")),
                List.of(button("📊 Свечи",      ":candles")),
                List.of(button("📈 Плечо",      ":leverage")),
                List.of(button("▶️ Запустить",   ":run")),
                List.of(button("‹ Назад",       ":back"))
        )).build();
        // заменить callbackData для «Назад»
        kb.getKeyboard().get(kb.getKeyboard().size() - 1).get(0)
                .setCallbackData("ai_trading_settings");

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text.toString())
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    private InlineKeyboardButton button(String text, String suffix) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(name() + suffix)
                .build();
    }

    private Map<String, Object> settingsToMap(StrategySettings settings) {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            BeanInfo info = Introspector.getBeanInfo(settings.getClass(), Object.class);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                Method read = pd.getReadMethod();
                if (read == null) continue;
                Class<?> type = pd.getPropertyType();
                if (isSimple(type)) {
                    try {
                        Object val = read.invoke(settings);
                        map.put(pd.getName(), val == null ? "null" : val);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private boolean isSimple(Class<?> cls) {
        return cls.isPrimitive()
                || Number.class.isAssignableFrom(cls)
                || CharSequence.class.isAssignableFrom(cls)
                || Boolean.class.isAssignableFrom(cls)
                || Enum.class.isAssignableFrom(cls)
                || Duration.class.isAssignableFrom(cls)
                || Date.class.isAssignableFrom(cls);
    }

    @Override
    public @NonNull String handleInput(Update u) {
        Long chatId = u.getCallbackQuery().getMessage().getChatId();
        String data = u.getCallbackQuery().getData();
        if (data.endsWith(":period"))     return "ai_trading_backtesting_set_period";
        if (data.endsWith(":commission")) return "ai_trading_backtesting_set_commission";
        if (data.endsWith(":timeframe"))  return "ai_trading_backtesting_set_timeframe";
        if (data.endsWith(":candles"))    return "ai_trading_backtesting_set_candles";
        if (data.endsWith(":leverage"))   return "ai_trading_backtesting_set_leverage";
        if (data.endsWith(":run")) {
            BacktestResult result = backtestService.runBacktest(chatId);
            menuService.deferNotice(chatId, buildSummary(result));
            return name();
        }
        if (data.endsWith(":back"))       return "ai_trading_settings";
        return name();
    }

    private String buildSummary(BacktestResult result) {
        double pnl     = result.getTotalPnl() * 100.0;
        double winRate = result.getWinRate()  * 100.0;
        int    count   = result.getTotalTrades();

        var sb = new StringBuilder()
                .append("*📈 Результаты бэктеста:*\n")
                .append(String.format("• Сделок: `%d`\n", count))
                .append(String.format("• Win-rate: `%.2f%%`\n", winRate))
                .append(String.format("• Общий PnL: `%.2f%%`\n", pnl));

        var losers = result.getLosingSymbols();
        if (!losers.isEmpty()) {
            sb.append("\n⚠️ Убыточные пары:\n");
            losers.forEach(sym -> sb.append("• `").append(sym).append("`\n"));
        }
        sb.append("\n💡 Проверьте TP/SL и параметры стратегии при необходимости.");
        return sb.toString();
    }
}
