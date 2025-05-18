// src/main/java/com/chicu/trader/trading/OrderService.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.service.ApiCredentials;
import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.util.SignatureUtil;
import com.chicu.trader.model.TradeLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final WebClient.Builder   webClientBuilder;
    private final UserSettingsService userSettings;
    private final BalanceService      balanceService;

    private static final int MAX_SLOTS = 5;

    public TradeLog openPosition(StrategyContext ctx) {
        Long chatId = ctx.getChatId();

        // получаем из БД: apiKey, secretKey и режим (REAL или TESTNET)
        ApiCredentials creds    = userSettings.getApiCredentials(chatId);
        boolean        isTestnet = userSettings.isTestnet(chatId);

        // строим WebClient с нужным базовым URL и заголовком API key
        String base = isTestnet
                ? "https://testnet.binance.vision"
                : "https://api.binance.com";
        WebClient client = webClientBuilder
                .baseUrl(base)
                .defaultHeader("X-MBX-APIKEY", creds.getApiKey())
                .build();

        // рассчитываем объёмы
        String symbol      = ctx.getSymbol();
        double price       = ctx.getPrice();
        double available   = balanceService.getAvailableUsdt(chatId);
        double usdtPerSlot = available / MAX_SLOTS;
        double quantity    = usdtPerSlot / price;

        // MARKET BUY
        Map<String,String> buyParams = new LinkedHashMap<>();
        buyParams.put("symbol",        symbol);
        buyParams.put("side",          "BUY");
        buyParams.put("type",          "MARKET");
        buyParams.put("quoteOrderQty", String.format("%.2f", usdtPerSlot));
        buyParams.put("timestamp",     String.valueOf(Instant.now().toEpochMilli()));
        String buyQuery = SignatureUtil.sign(buyParams, creds.getSecretKey());
        client.post()
                .uri(uri -> uri.path("/api/v3/order").query(buyQuery).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // OCO SELL (TP/SL)
        Map<String,String> ocoParams = new LinkedHashMap<>();
        ocoParams.put("symbol",               symbol);
        ocoParams.put("side",                 "SELL");
        ocoParams.put("quantity",             String.format("%.6f", quantity));
        ocoParams.put("price",                String.format("%.8f", ctx.getTpPrice()));
        ocoParams.put("stopPrice",            String.format("%.8f", ctx.getSlPrice()));
        ocoParams.put("stopLimitPrice",       String.format("%.8f", ctx.getSlPrice()));
        ocoParams.put("stopLimitTimeInForce","GTC");
        ocoParams.put("timestamp",            String.valueOf(Instant.now().toEpochMilli()));
        String ocoQuery = SignatureUtil.sign(ocoParams, creds.getSecretKey());
        client.post()
                .uri(uri -> uri.path("/api/v3/order/oco").query(ocoQuery).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ctx.toEntryLog();
    }

    public Optional<TradeLog> checkAndClose(StrategyContext ctx) {
        return ctx.getExitLog();
    }
}
