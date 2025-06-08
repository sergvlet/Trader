package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.trading.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpBinanceOrderService implements OrderService {

    private static final String PROD_REST = "https://api.binance.com";
    private static final String TEST_REST = "https://testnet.binance.vision";

    private final UserSettingsService userSettingsService;
    private final HttpClient         httpClient = HttpClient.newHttpClient();

    private String baseUrl(Long chatId) {
        boolean isTest = userSettingsService.isTestnet(chatId);
        String base = isTest ? TEST_REST : PROD_REST;
        log.debug("HttpBinanceOrderService: base URL for chatId={} is {}", chatId, base);
        return base;
    }

    private String apiKey(Long chatId) {
        String key = userSettingsService.getApiCredentials(chatId).getApiKey();
        log.debug("HttpBinanceOrderService: retrieved API key for chatId={}", chatId);
        return key;
    }

    private String secretKey(Long chatId) {
        String secret = userSettingsService.getApiCredentials(chatId).getSecretKey();
        log.debug("HttpBinanceOrderService: retrieved secret key for chatId={}", chatId);
        return secret;
    }

    @Override
    public void placeMarketOrder(Long chatId, String symbol, double quantity) throws BinanceOrderException {
        log.info("placeMarketOrder ▶ {} MARKET {}", chatId, symbol);
        String path = "/api/v3/order";
        long ts = Instant.now().toEpochMilli();

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",    symbol.toUpperCase());
        params.put("side",      "BUY");
        params.put("type",      "MARKET");
        params.put("quantity",  String.valueOf(quantity));
        params.put("timestamp", String.valueOf(ts));

        sendSignedRequest(chatId, path, params);
    }

    @Override
    public boolean placeOcoOrder(Long chatId,
                                 String symbol,
                                 double quantity,
                                 double stopPrice,
                                 double limitPrice) throws BinanceOrderException {
        log.info("placeOcoOrder ▶ {} OCO {} qty={} SL={} TP={}",
                 chatId, symbol, quantity, stopPrice, limitPrice);

        // Меняем путь на современный OCO-эндпоинт
        String path = "/api/v3/order/oco";
        long ts = Instant.now().toEpochMilli();

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",               symbol.toUpperCase());
        params.put("side",                 "SELL");
        params.put("quantity",             String.valueOf(quantity));
        params.put("price",                String.valueOf(limitPrice));
        params.put("stopPrice",            String.valueOf(stopPrice));
        params.put("stopLimitPrice",       String.valueOf(stopPrice));
        params.put("stopLimitTimeInForce", "GTC");

        // Дополнительно можно задать свои clientOrderId, чтобы было проще отслеживать
        String clientId = "oco_" + chatId + "_" + ts;
        params.put("listClientOrderId",  clientId);
        params.put("limitClientOrderId", clientId + "_L");
        params.put("stopClientOrderId",  clientId + "_S");

        params.put("timestamp",           String.valueOf(ts));

        sendSignedRequest(chatId, path, params);
        return true;
    }

    private String sendSignedRequest(Long chatId,
                                     String path,
                                     Map<String,String> params) throws BinanceOrderException {
        try {
            // 1) Собираем строку параметров
            String qs = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));

            // 2) Подписываем
            String signature = HmacSHA256Signer.sign(qs, secretKey(chatId));
            String fullQs = qs + "&signature=" + signature;

            // 3) Формируем и отправляем POST-запрос
            String url = baseUrl(chatId) + path + "?" + fullQs;
            log.debug("sendSignedRequest ▶ URL={}", url);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-MBX-APIKEY", apiKey(chatId))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            String body = resp.body();

            if (status >= 200 && status < 300) {
                log.info("Binance [{}] {} → {} {}", chatId, path, status, body);
                return body;
            } else {
                log.error("Binance [{}] {} FAILED: {} {}", chatId, path, status, body);
                throw new BinanceOrderException("Binance returned HTTP " + status + ": " + body);
            }

        } catch (BinanceOrderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in sendSignedRequest ▶ chatId={}", chatId, e);
            throw new BinanceOrderException("Error sending request", e);
        }
    }
}
