package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolFilter;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    @Getter
    private final String       baseUrl;

    private ExchangeInfo exchangeInfo;

    /** Public market-data only */
    public BinanceHttpClient(String baseUrl) {
        this.apiKey    = null;
        this.secretKey = null;
        this.baseUrl   = Objects.requireNonNull(baseUrl);
        log.info("BinanceHttpClient initialized (public): {}", baseUrl);
    }

    /** Private (signed) client */
    public BinanceHttpClient(String apiKey, String secretKey, String baseUrl) {
        this.apiKey    = Objects.requireNonNull(apiKey);
        this.secretKey = Objects.requireNonNull(secretKey);
        this.baseUrl   = Objects.requireNonNull(baseUrl);
        log.info("BinanceHttpClient initialized (private): {}", baseUrl);
    }

    public ExchangeInfo getExchangeInfo() {
        try {
            String json = restTemplate.getForObject(baseUrl + INFO_EP, String.class);
            this.exchangeInfo = objectMapper.readValue(json, ExchangeInfo.class);
            return exchangeInfo;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getExchangeInfo", e);
        }
    }

    public BigDecimal getLastPrice(String symbol) {
        try {
            String url  = baseUrl + PRICE_EP + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String json = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(json);
            return new BigDecimal(node.get("price").asText());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getLastPrice", e);
        }
    }

    public BigDecimal getBalance(String asset) {
        requireKeys();
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

    /**
     * Market BUY: возвращает JSON-ответ Binance
     */
    public String placeMarketBuy(String symbol, BigDecimal qty) {
        requireKeys();
        Map<String,String> params = Map.of(
                "symbol",   symbol,
                "side",     "BUY",
                "type",     "MARKET",
                "quantity", qty.stripTrailingZeros().toPlainString()
        );
        String json = sendSigned(HttpMethod.POST, ORDER_EP, params);
        log.info("MARKET BUY {} qty={}", symbol, qty);
        return json;
    }

    /**
     * Market SELL: возвращает JSON-ответ Binance
     */
    public String placeMarketSell(String symbol, BigDecimal qty) {
        requireKeys();
        Map<String,String> params = Map.of(
                "symbol",   symbol,
                "side",     "SELL",
                "type",     "MARKET",
                "quantity", qty.stripTrailingZeros().toPlainString()
        );
        String json = sendSigned(HttpMethod.POST, ORDER_EP, params);
        log.info("MARKET SELL {} qty={}", symbol, qty);
        return json;
    }

    /**
     * OCO SELL: возвращает JSON-ответ Binance
     */
    public String placeOcoSell(String symbol,
                               BigDecimal qty,
                               BigDecimal stopLossPrice,
                               BigDecimal takeProfitPrice) {
        requireKeys();
        if (exchangeInfo == null) getExchangeInfo();

        SymbolInfo info = exchangeInfo.getSymbols().stream()
                .filter(s -> s.getSymbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbol));

        BigDecimal tickSize = info.getFilters().stream()
                .filter(f -> "PRICE_FILTER".equals(f.getFilterType()))
                .map(SymbolFilter::getTickSize).map(BigDecimal::new)
                .findFirst().orElseThrow();
        BigDecimal stepSize = info.getFilters().stream()
                .filter(f -> "LOT_SIZE".equals(f.getFilterType()))
                .map(SymbolFilter::getStepSize).map(BigDecimal::new)
                .findFirst().orElseThrow();

        int priceScale = tickSize.stripTrailingZeros().scale();
        int qtyScale   = stepSize.stripTrailingZeros().scale();

        BigDecimal quantity = qty.setScale(qtyScale, RoundingMode.DOWN);
        if (quantity.compareTo(stepSize) < 0) {
            throw new RuntimeException("Quantity too small for symbol " + symbol);
        }

        BigDecimal lastPrice = getLastPrice(symbol);
        BigDecimal rawTP = takeProfitPrice.setScale(priceScale, RoundingMode.DOWN);
        BigDecimal rawSP = stopLossPrice  .setScale(priceScale, RoundingMode.UP);
        BigDecimal tick   = tickSize.stripTrailingZeros();

        BigDecimal minTP = lastPrice.add(tick).setScale(priceScale, RoundingMode.DOWN);
        if (rawTP.compareTo(minTP) < 0) rawTP = minTP;
        BigDecimal maxSP = lastPrice.subtract(tick).setScale(priceScale, RoundingMode.UP);
        if (rawSP.compareTo(maxSP) > 0) rawSP = maxSP;
        BigDecimal rawSLimit = rawSP.subtract(tick).setScale(priceScale, RoundingMode.DOWN);

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",             symbol);
        params.put("side",               "SELL");
        params.put("quantity",           quantity.toPlainString());
        params.put("price",              rawTP.toPlainString());
        params.put("stopPrice",          rawSP.toPlainString());
        params.put("stopLimitPrice",     rawSLimit.toPlainString());
        params.put("stopLimitTimeInForce","GTC");

        try {
            String json = sendSigned(HttpMethod.POST, OCO_ORDER_EP, params);
            log.info("OCO SELL {} qty={} TP={} stopPrice={} stopLimit={}",
                    symbol, quantity, rawTP, rawSP, rawSLimit);
            return json;
        } catch (HttpClientErrorException.BadRequest ex) {
            String body = ex.getResponseBodyAsString();
            if (body.contains("\"code\":-2010")) {
                log.warn("OCO rejected ({}), executing MARKET SELL instead", body);
                return placeMarketSell(symbol, quantity);
            }
            throw ex;
        }
    }

    // --------------------------

    private long getServerTime() {
        try {
            String json = restTemplate.getForObject(baseUrl + TIME_EP, String.class);
            JsonNode n = objectMapper.readTree(json);
            return n.get("serverTime").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getServerTime", e);
        }
    }

    String sendSigned(HttpMethod method, String path, Map<String, String> params) {
        requireKeys();
        Map<String,String> q = new HashMap<>(params);
        q.put("timestamp",  String.valueOf(getServerTime()));
        q.put("recvWindow", String.valueOf(DEFAULT_WINDOW));

        String query = q.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String sig = generateSignature(secretKey, query);
        String url = baseUrl + path + "?" + query + "&signature=" + sig;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);

        ResponseEntity<String> resp =
                restTemplate.exchange(url, method, new HttpEntity<>(headers), String.class);
        return resp.getBody();
    }

    private void requireKeys() {
        if (apiKey == null || secretKey == null) {
            throw new IllegalStateException("This operation requires API key and secret");
        }
    }

    private String generateSignature(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка generateSignature", e);
        }
    }

    public String startUserDataStream() {
        return sendSigned(HttpMethod.POST, "/api/v3/userDataStream", Map.of());
    }

    public void keepAliveUserDataStream(String listenKey) {
        sendSigned(HttpMethod.PUT, "/api/v3/userDataStream", Map.of("listenKey", listenKey));
    }
}
