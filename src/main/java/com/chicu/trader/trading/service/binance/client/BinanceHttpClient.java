package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class BinanceHttpClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Эти три поля будут установлены конструктором фабрики:
    private String apiKey;
    private String secretKey;
    private String baseUrl;

    /**
     * Дефолтный конструктор — инициализирует RestTemplate.
     * Поля apiKey/secretKey/baseUrl останутся null, если вызывается напрямую.
     */
    public BinanceHttpClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Конструктор для создания из фабрики.
     */
    public BinanceHttpClient(String apiKey, String secretKey, String baseUrl) {
        this(); 
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
        log.info("BinanceHttpClient initialized via factory: baseUrl={}, apiKey={}", baseUrl, apiKey);
    }

    // =====================================================================================
    // Методы, уже были
    // =====================================================================================

    public String sendPublicGet(String url) {
        return restTemplate.getForObject(url, String.class);
    }

    public String sendSignedGet(String url, String apiKey, String secretKey, Map<String, String> params) {
        String query = buildQuery(params);
        String signature = generateSignature(secretKey, query);
        String fullUrl = url + "?" + query + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    public String sendSignedPost(String url, String apiKey, String secretKey, Map<String, String> params) {
        String query = buildQuery(params);
        String signature = generateSignature(secretKey, query);
        String fullUrl = url + "?" + query + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, entity, String.class);
        return response.getBody();
    }

    // =====================================================================================
    // Новые методы для BinanceRestClient
    // =====================================================================================

    /** Информация об бирже (списки пар, лимиты и т.п.) */
    public ExchangeInfo getExchangeInfo() {
        try {
            String url = baseUrl + "/api/v3/exchangeInfo";
            String json = sendPublicGet(url);
            return objectMapper.readValue(json, ExchangeInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при getExchangeInfo", e);
        }
    }

    /** Текущая цена символа */
    public BigDecimal getLastPrice(String symbol) {
        try {
            String url = baseUrl + "/api/v3/ticker/price?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String json = sendPublicGet(url);
            JsonNode node = objectMapper.readTree(json);
            return new BigDecimal(node.get("price").asText());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при getLastPrice", e);
        }
    }

    /** Баланс по активу */
    public BigDecimal getBalance(String asset) {
        try {
            String url = baseUrl + "/api/v3/account";
            Map<String,String> params = new HashMap<>();
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));
            String json = sendSignedGet(url, apiKey, secretKey, params);

            JsonNode balances = objectMapper.readTree(json).get("balances");
            for (JsonNode b : balances) {
                if (asset.equals(b.get("asset").asText())) {
                    return new BigDecimal(b.get("free").asText());
                }
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при getBalance", e);
        }
    }

    /** Рыночная покупка */
    public void placeMarketBuy(String symbol, BigDecimal qty) {
        String url = baseUrl + "/api/v3/order";
        Map<String,String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", "BUY");
        params.put("type", "MARKET");
        params.put("quantity", qty.toPlainString());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        sendSignedPost(url, apiKey, secretKey, params);
        log.info("BINANCE HTTP: MARKET BUY {} qty={}", symbol, qty);
    }

    /** Рыночная продажа */
    public void placeMarketSell(String symbol, BigDecimal qty) {
        String url = baseUrl + "/api/v3/order";
        Map<String,String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", "SELL");
        params.put("type", "MARKET");
        params.put("quantity", qty.toPlainString());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        sendSignedPost(url, apiKey, secretKey, params);
        log.info("BINANCE HTTP: MARKET SELL {} qty={}", symbol, qty);
    }

    /** OCO-ордер на продажу */
    public void placeOcoSell(String symbol, BigDecimal qty, BigDecimal stopLossPrice, BigDecimal takeProfitPrice) {
        String url = baseUrl + "/api/v3/order/oco";
        Map<String,String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", "SELL");
        params.put("type", "OCO");
        params.put("quantity", qty.toPlainString());
        params.put("stopPrice", stopLossPrice.toPlainString());
        params.put("price", takeProfitPrice.toPlainString());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        sendSignedPost(url, apiKey, secretKey, params);
        log.info("BINANCE HTTP: OCO SELL {} qty={} SL={} TP={}", symbol, qty, stopLossPrice, takeProfitPrice);
    }

    // =====================================================================================
    // Private helpers
    // =====================================================================================

    private String buildQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }

    private String generateSignature(String secretKey, String query) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = hmac.doFinal(query.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации сигнатуры", e);
        }
    }
}
