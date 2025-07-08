package com.chicu.trader.bot.menu.feature.ai_trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.menu.core.MenuService;
import com.chicu.trader.bot.menu.core.MenuState;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.backtest.BacktestResult;
import com.chicu.trader.trading.backtest.GeneticOptimizerService;
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

    private final AiTradingSettingsService   aiSettingsService;
    private final ProfitablePairService      pairService;
    private final BacktestSettingsService    btService;
    private final BacktestService            backtestService;
    private final StrategyRegistry           strategyRegistry;
    private final GeneticOptimizerService    geneticOptimizerService;
    private final MenuService                menuService;

    public AiTradingBacktestingConfigState(
            AiTradingSettingsService   aiSettingsService,
            ProfitablePairService      pairService,
            BacktestSettingsService    btService,
            BacktestService            backtestService,
            StrategyRegistry           strategyRegistry,
            GeneticOptimizerService    geneticOptimizerService,
            @Lazy MenuService          menuService
    ) {
        this.aiSettingsService       = aiSettingsService;
        this.pairService             = pairService;
        this.btService               = btService;
        this.backtestService         = backtestService;
        this.strategyRegistry        = strategyRegistry;
        this.geneticOptimizerService = geneticOptimizerService;
        this.menuService             = menuService;
    }

    @Override
    public String name() {
        return "ai_trading_backtesting_config";
    }

    @Override
    public SendMessage render(Long chatId) {
        AiTradingSettings ai = aiSettingsService.getSettingsOrThrow(chatId);
        String strategyName = ai.getStrategy().name();

        // Настройки стратегии
        StrategySettings stratSettings =
                strategyRegistry.getSettings(StrategyType.valueOf(strategyName), chatId);
        Map<String, Object> stratMap = settingsToMap(stratSettings);
        StringBuilder stratSection = new StringBuilder("*🛠 Настройки стратегии:*\n");
        stratMap.forEach((k, v) ->
                stratSection.append("• ").append(k).append(": `").append(v).append("`\n")
        );
        stratSection.append("\n");

        // Список пар
        List<String> symbols = Optional.ofNullable(ai.getSymbols())
                .filter(s -> !s.isBlank())
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .toList())
                .orElseGet(() -> pairService.getActivePairs(chatId).stream()
                        .map(ProfitablePair::getSymbol)
                        .toList());
        String pairsList = symbols.isEmpty() ? "— не выбраны —" : String.join(", ", symbols);

        // Параметры бэктеста
        BacktestSettings cfg = btService.getOrCreate(chatId);

        // Формируем текст меню
        StringBuilder text = new StringBuilder()
                .append("*✅ Стратегия:* `").append(strategyName).append("`\n\n")
                .append(stratSection)
                .append("*🔀 Пары:* `").append(pairsList).append("`\n\n")
                .append("*⚙️ Настройки бэктеста:*\n")
                .append(String.format("• Период: `%s` → `%s`\n",
                        cfg.getStartDate().format(DF), cfg.getEndDate().format(DF)))
                .append(String.format("• Комиссия: `%.2f%%`\n", cfg.getCommissionPct()))
                .append(String.format("• Проскальзывание: `%.2f%%`\n", cfg.getSlippagePct()))
                .append(String.format("• Таймфрейм: `%s`\n", cfg.getTimeframe()))
                .append(String.format("• Свечей (кэш limit): `%d`\n", cfg.getCachedCandlesLimit()))
                .append(String.format("• Плечо: `%dx`\n\n", cfg.getLeverage()))
                .append("Выберите, что хотите изменить:");

        // Выводим уведомление о предыдущем результате, если есть
        menuService.popNotice(chatId).ifPresent(msg -> text.append("\n\n").append(msg.getText()));


        // Кнопки: три в ряду
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(
                        btn("📅 Период", ":period"),
                        btn("💰 Комиссия", ":commission"),
                        btn("🛢 Проскальзывание", ":slippage")
                ),
                List.of(
                        btn("⏱ Таймфрейм", ":timeframe"),
                        btn("📊 Свечи", ":candles"),
                        btn("📈 Плечо", ":leverage")
                ),
                List.of(
                        btn("▶️ Запустить", ":run"),
                        btn("🧬 Эволюция", ":evolve"),
                        btn("‹ Назад", ":back")
                )
        )).build();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text.toString())
                .parseMode("Markdown")
                .replyMarkup(kb)
                .build();
    }

    private InlineKeyboardButton btn(String text, String suffix) {
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
                if (type.isPrimitive()
                        || Number.class.isAssignableFrom(type)
                        || CharSequence.class.isAssignableFrom(type)
                        || Boolean.class.isAssignableFrom(type)
                        || Enum.class.isAssignableFrom(type)
                        || Duration.class.isAssignableFrom(type)
                        || Date.class.isAssignableFrom(type)) {
                    Object val = read.invoke(settings);
                    map.put(pd.getName(), val != null ? val : "null");
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    @Override
    public @NonNull String handleInput(Update u) {
        String data  = u.getCallbackQuery().getData();
        long   chatId = u.getCallbackQuery().getMessage().getChatId();

        if (data.endsWith(":period"))     return "ai_trading_backtesting_set_period";
        if (data.endsWith(":commission")) return "ai_trading_backtesting_set_commission";
        if (data.endsWith(":slippage"))   return "ai_trading_backtesting_set_slippage";
        if (data.endsWith(":timeframe"))  return "ai_trading_backtesting_set_timeframe";
        if (data.endsWith(":candles"))    return "ai_trading_backtesting_set_candles";
        if (data.endsWith(":leverage"))   return "ai_trading_backtesting_set_leverage";

        if (data.endsWith(":run")) {
            BacktestResult result = backtestService.runBacktest(chatId);
            menuService.deferNotice(chatId, buildSummary(result));
            return name();
        }

        if (data.endsWith(":evolve")) {
            String summary = geneticOptimizerService.optimizeEvolutionarySync(chatId);
            menuService.deferNotice(chatId, summary);
            return name();
        }

        if (data.endsWith(":back")) {
            return "ai_trading_settings";
        }

        return name();
    }

    private String buildSummary(BacktestResult result) {
        double pnl     = result.getTotalPnl() * 100.0;
        double winRate = result.getWinRate()  * 100.0;
        int    count   = result.getTotalTrades();

        StringBuilder sb = new StringBuilder()
                .append("*📈 Результаты бэктеста:*\n")
                .append(String.format("• Сделок: `%d`\n",       count))
                .append(String.format("• Win-rate: `%.2f%%`\n", winRate))
                .append(String.format("• Общий PnL: `%.2f%%`\n", pnl));

        List<String> losers = result.getLosingSymbols();
        if (!losers.isEmpty()) {
            sb.append("\n⚠️ Убыточные пары:\n");
            losers.forEach(sym -> sb.append("• `").append(sym).append("`\n"));
        }
        sb.append("\n💡 Проверьте TP/SL и параметры стратегии при необходимости.");
        return sb.toString();
    }
}
