package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP-клиент для работы с Binance REST API.
 * Может быть инициализирован либо с ключами (полный доступ),
 * либо только с baseUrl для публичных запросов (market-data).
 */
@Slf4j
public class BinanceHttpClient {

    private static final String TIME_EP        = "/api/v3/time";
    private static final String INFO_EP        = "/api/v3/exchangeInfo";
    private static final String PRICE_EP       = "/api/v3/ticker/price";
    private static final String ACCOUNT_EP     = "/api/v3/account";
    private static final String ORDER_EP       = "/api/v3/order";
    private static final String OCO_ORDER_EP   = "/api/v3/order/oco";
    private static final long   DEFAULT_WINDOW = 5_000L;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String       apiKey;
    private final String       secretKey;
    private final String       baseUrl;

    /** Кэш загруженного ExchangeInfo */
    private ExchangeInfo       exchangeInfo;

    /**
     * Полный конструктор с API-ключами — для торговли и чтения аккаунта.
     */
    public BinanceHttpClient(String apiKey, String secretKey, String baseUrl) {
        this.apiKey    = Objects.requireNonNull(apiKey, "apiKey");
        this.secretKey = Objects.requireNonNull(secretKey, "secretKey");
        this.baseUrl   = Objects.requireNonNull(baseUrl, "baseUrl");
        log.info("BinanceHttpClient initialized (private): {}", baseUrl);
    }

    /**
     * Публичный конструктор без ключей — только market-data эндпоинты.
     */
    public BinanceHttpClient(String baseUrl) {
        this.apiKey    = null;
        this.secretKey = null;
        this.baseUrl   = Objects.requireNonNull(baseUrl, "baseUrl");
        log.info("BinanceHttpClient initialized (public): {}", baseUrl);
    }

    // ---------------- Public API ----------------

    /** Информация об обмене (списки пар и фильтров) */
    public ExchangeInfo getExchangeInfo() {
        try {
            String json = restTemplate.getForObject(baseUrl + INFO_EP, String.class);
            this.exchangeInfo = objectMapper.readValue(json, ExchangeInfo.class);
            return exchangeInfo;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getExchangeInfo", e);
        }
    }

    /** Текущая цена символа */
    public BigDecimal getLastPrice(String symbol) {
        try {
            String url  = baseUrl + PRICE_EP
                          + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String json = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(json);
            return new BigDecimal(node.get("price").asText());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getLastPrice", e);
        }
    }

    /** Баланс по активу (требует ключей) */
    public BigDecimal getBalance(String asset) {
        String json = sendSigned(HttpMethod.GET, ACCOUNT_EP, Collections.emptyMap());
        try {
            JsonNode arr = objectMapper.readTree(json).get("balances");
            for (JsonNode b : arr) {
                if (asset.equals(b.get("asset").asText())) {
                    return new BigDecimal(b.get("free").asText());
                }
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getBalance", e);
        }
    }

    /** Рынoчный BUY (требует ключей) */
    public void placeMarketBuy(String symbol, BigDecimal qty) {
        requireKeys();
        Map<String,String> p = Map.of(
            "symbol",   symbol,
            "side",     "BUY",
            "type",     "MARKET",
            "quantity", qty.stripTrailingZeros().toPlainString()
        );
        sendSigned(HttpMethod.POST, ORDER_EP, p);
        log.info("MARKET BUY {} qty={}", symbol, qty);
    }

    /** Рынoчный SELL (требует ключей) */
    public void placeMarketSell(String symbol, BigDecimal qty) {
        requireKeys();
        Map<String,String> p = Map.of(
            "symbol",   symbol,
            "side",     "SELL",
            "type",     "MARKET",
            "quantity", qty.stripTrailingZeros().toPlainString()
        );
        sendSigned(HttpMethod.POST, ORDER_EP, p);
        log.info("MARKET SELL {} qty={}", symbol, qty);
    }

    /** OCO SELL (требует ключей) */
    public void placeOcoSell(String symbol,
                             BigDecimal qty,
                             BigDecimal stopLossPrice,
                             BigDecimal takeProfitPrice) {
        // при первом вызове подгружаем exchangeInfo
        if (exchangeInfo == null) getExchangeInfo();

        // находим информацию по символу
        SymbolInfo info = exchangeInfo.getSymbols().stream()
            .filter(s -> s.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbol));

        // извлекаем tickSize и stepSize из фильтров
        BigDecimal tickSize = info.getFilters().stream()
            .filter(f -> "PRICE_FILTER".equals(f.getFilterType()))
            .map(SymbolFilter::getTickSize)
            .map(BigDecimal::new)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("PRICE_FILTER not found: " + symbol));

        BigDecimal stepSize = info.getFilters().stream()
            .filter(f -> "LOT_SIZE".equals(f.getFilterType()))
            .map(SymbolFilter::getStepSize)
            .map(BigDecimal::new)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("LOT_SIZE not found: " + symbol));

        int pricePrec = tickSize.stripTrailingZeros().scale();
        int qtyPrec   = stepSize.stripTrailingZeros().scale();

        // округляем вниз
        BigDecimal tp = takeProfitPrice.setScale(pricePrec, RoundingMode.DOWN);
        BigDecimal sl = stopLossPrice .setScale(pricePrec, RoundingMode.DOWN);
        BigDecimal q  = qty           .setScale(qtyPrec,   RoundingMode.DOWN);

        requireKeys();
        Map<String,String> p = new LinkedHashMap<>();
        p.put("symbol",               symbol);
        p.put("side",                 "SELL");
        p.put("quantity",             q.stripTrailingZeros().toPlainString());
        p.put("price",                tp.stripTrailingZeros().toPlainString());
        p.put("stopPrice",            sl.stripTrailingZeros().toPlainString());
        p.put("stopLimitPrice",       sl.stripTrailingZeros().toPlainString());
        p.put("stopLimitTimeInForce", "GTC");

        sendSigned(HttpMethod.POST, OCO_ORDER_EP, p);
        log.info("OCO SELL {} qty={} SL={} TP={}",
            symbol, q.toPlainString(), sl.toPlainString(), tp.toPlainString()
        );
    }

    // ---------------- Internal ----------------

    private long getServerTime() {
        try {
            String json = restTemplate.getForObject(baseUrl + TIME_EP, String.class);
            JsonNode node = objectMapper.readTree(json);
            return node.get("serverTime").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getServerTime", e);
        }
    }

    private String sendSigned(HttpMethod method,
                              String path,
                              Map<String,String> params) {
        requireKeys();

        Map<String,String> q = new HashMap<>(params);
        q.put("timestamp",  String.valueOf(getServerTime()));
        q.put("recvWindow", String.valueOf(DEFAULT_WINDOW));

        String query = q.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "="
                + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .reduce((a,b) -> a + "&" + b)
            .orElse("");

        String sig = generateSignature(secretKey, query);
        String url = baseUrl + path + "?" + query + "&signature=" + sig;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);

        ResponseEntity<String> resp = restTemplate.exchange(
            url, method, new HttpEntity<>(headers), String.class
        );
        return resp.getBody();
    }

    private void requireKeys() {
        if (apiKey == null || secretKey == null) {
            throw new IllegalStateException(
                "This operation requires API key and secret"
            );
        }
    }

    private String generateSignature(String secretKey, String query) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(
                new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
                                  "HmacSHA256")
            );
            byte[] hash = hmac.doFinal(query.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка generateSignature", e);
        }
    }
}
