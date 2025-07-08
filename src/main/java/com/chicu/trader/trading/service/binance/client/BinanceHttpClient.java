package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.service.binance.client.model.ExchangeInfo;
import com.chicu.trader.trading.service.binance.client.model.SymbolFilter;
import com.chicu.trader.trading.service.binance.client.model.SymbolInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class BinanceHttpClient {

    private static final String TIME_EP = "/api/v3/time";
    private static final String INFO_EP = "/api/v3/exchangeInfo";
    private static final String PRICE_EP = "/api/v3/ticker/price";
    private static final String ACCOUNT_EP = "/api/v3/account";
    private static final String ORDER_EP = "/api/v3/order";
    private static final String OCO_ORDER_EP = "/api/v3/order/oco";
    private static final long DEFAULT_WINDOW = 5000L;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String secretKey;
    @Getter
    private final String baseUrl;

    private final Map<String, LotRule> lotRules = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> minNotionalRules = new ConcurrentHashMap<>();
    private ExchangeInfo exchangeInfo;

    public BinanceHttpClient(String baseUrl) {
        this.apiKey = null;
        this.secretKey = null;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        log.info("BinanceHttpClient initialized (public): {}", baseUrl);
    }

    public BinanceHttpClient(String apiKey, String secretKey, String baseUrl) {
        this.apiKey = Objects.requireNonNull(apiKey);
        this.secretKey = Objects.requireNonNull(secretKey);
        this.baseUrl = Objects.requireNonNull(baseUrl);
        log.info("BinanceHttpClient initialized (private): {}", baseUrl);
    }

    private synchronized void ensureExchangeInfoLoaded() {
        if (exchangeInfo != null) return;
        try {
            String json = restTemplate.getForObject(baseUrl + INFO_EP, String.class);
            exchangeInfo = objectMapper.readValue(json, ExchangeInfo.class);

            for (SymbolInfo s : exchangeInfo.getSymbols()) {
                String symbol = s.getSymbol();

                for (SymbolFilter f : s.getFilters()) {
                    switch (f.getFilterType()) {
                        case "LOT_SIZE" -> {
                            BigDecimal step = f.getStepSizeAsDecimal();
                            BigDecimal min  = f.getMinQtyAsDecimal();
                            if (step != null && min != null) {
                                lotRules.put(symbol, new LotRule(step, min));
                            } else {
                                log.warn("⚠️ LOT_SIZE stepSize or minQty is null for {}", symbol);
                            }
                        }
                        case "MIN_NOTIONAL", "NOTIONAL" -> {
                            BigDecimal minNotional = f.getMinNotionalAsDecimal();
                            if (minNotional != null) {
                                minNotionalRules.put(symbol, minNotional);
                            } else {
                                log.warn("⚠️ {} value is null for {}", f.getFilterType(), symbol);
                            }
                        }
                        default -> {
                            // Не обрабатываем другие фильтры
                        }
                    }
                }
            }

            log.info("Loaded trading rules (LOT_SIZE: {}, MIN_NOTIONAL: {}) for {} symbols",
                    lotRules.size(), minNotionalRules.size(), exchangeInfo.getSymbols().size());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки exchangeInfo", e);
        }
    }

    private BigDecimal normalizeQuantity(String symbol, BigDecimal rawQty) {
        ensureExchangeInfoLoaded();
        LotRule rule = lotRules.get(symbol);
        if (rule == null) {
            log.warn("No LOT_SIZE rule for {}, using raw qty={}", symbol, rawQty);
            return rawQty;
        }
        BigDecimal disp = rawQty.divide(rule.stepSize, 0, RoundingMode.DOWN).multiply(rule.stepSize);
        if (disp.compareTo(rule.minQty) < 0) {
            log.warn("Normalized qty {} < minQty {} for {}, will skip", disp, rule.minQty, symbol);
            return BigDecimal.ZERO;
        }
        return disp.stripTrailingZeros();
    }

    public BigDecimal getLastPrice(String symbol) {
        try {
            String url = baseUrl + PRICE_EP + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            String json = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(json);
            return new BigDecimal(node.get("price").asText());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getLastPrice", e);
        }
    }

    public String placeMarketBuy(String symbol, BigDecimal qty) {
        requireKeys(); // Проверка на наличие API ключей
        BigDecimal quantity = normalizeQuantity(symbol, qty);

        if (quantity.signum() == 0) {
            throw new IllegalArgumentException("Quantity after normalization is zero for " + symbol);
        }

        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", "BUY");
        params.put("type", "MARKET");
        params.put("quantity", quantity.toPlainString());

        try {
            String json = sendSigned(HttpMethod.POST, ORDER_EP, params);
            log.info("✅ MARKET BUY {} qty={}", symbol, quantity);
            return json;

        } catch (HttpClientErrorException.BadRequest ex) {
            String body = ex.getResponseBodyAsString();

            if (body.contains("Filter failure: NOTIONAL")) {
                log.warn("⚠️ Filter failure: NOTIONAL for {} with qty={}", symbol, quantity);

                BigDecimal minNotional = minNotionalRules.get(symbol);
                if (minNotional == null) {
                    log.error("❌ No MIN_NOTIONAL rule for {}, cannot retry", symbol);
                    throw ex;
                }

                BigDecimal price = getLastPrice(symbol);
                int qtyScale = lotRules.get(symbol).stepSize.stripTrailingZeros().scale();

                BigDecimal adjustedQty = minNotional.divide(price, qtyScale, RoundingMode.UP);
                adjustedQty = normalizeQuantity(symbol, adjustedQty);

                if (adjustedQty.signum() == 0) {
                    log.error("❌ Adjusted quantity is zero after normalization for {}", symbol);
                    throw ex;
                }

                BigDecimal cost = adjustedQty.multiply(price);
                BigDecimal balance = getBalance("USDT");

                if (balance.compareTo(cost) < 0) {
                    log.error("❌ Недостаточно средств: balance={} < required={} for {}", balance, cost, symbol);
                    throw new IllegalStateException("Недостаточно средств для покупки " + symbol);
                }

                log.warn("🔁 Retrying MARKET BUY {} with adjusted qty={} (minNotional={}, price={})",
                        symbol, adjustedQty, minNotional, price);
                return placeMarketBuy(symbol, adjustedQty);
            }

            throw ex; // Любая другая ошибка – пробрасываем дальше
        }
    }

    public String placeMarketSell(String symbol, BigDecimal qty) {
        requireKeys();
        ensureExchangeInfoLoaded();

        BigDecimal quantity = normalizeQuantity(symbol, qty);
        if (quantity.signum() == 0) {
            throw new IllegalArgumentException("📉 Quantity after normalization is zero for " + symbol);
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("side", "SELL");
            params.put("type", "MARKET");
            params.put("quantity", quantity.toPlainString());

            String json = sendSigned(HttpMethod.POST, ORDER_EP, params);
            log.info("✅ MARKET SELL {} qty={}", symbol, quantity);
            return json;

        } catch (HttpClientErrorException.BadRequest ex) {
            String body = ex.getResponseBodyAsString();

            if (body.contains("Filter failure: NOTIONAL")) {
                log.warn("⚠️ Filter failure: NOTIONAL on SELL {} with qty={}", symbol, quantity);

                BigDecimal minNotional = minNotionalRules.get(symbol);
                if (minNotional == null) {
                    log.error("❌ No MIN_NOTIONAL rule for {}, cannot retry", symbol);
                    throw ex;
                }

                BigDecimal price = getLastPrice(symbol);
                if (price == null || price.signum() == 0) {
                    log.error("❌ Invalid price for {}: {}", symbol, price);
                    throw ex;
                }

                LotRule rule = lotRules.get(symbol);
                if (rule == null) {
                    log.error("❌ No LOT_SIZE rule for {}, cannot compute adjusted qty", symbol);
                    throw ex;
                }

                int qtyScale = rule.stepSize.stripTrailingZeros().scale();
                BigDecimal adjustedQty = minNotional.divide(price, qtyScale, RoundingMode.UP);
                adjustedQty = normalizeQuantity(symbol, adjustedQty);

                if (adjustedQty.compareTo(quantity) <= 0) {
                    log.error("❌ Adjusted qty={} still too small (was={}) — giving up", adjustedQty, quantity);
                    throw ex;
                }

                log.warn("🔁 Retrying MARKET SELL {} with adjusted qty={} (minNotional={}, price={})",
                        symbol, adjustedQty, minNotional, price);

                return placeMarketSellRetry(symbol, adjustedQty);
            }

            throw ex;
        }
    }

    private String placeMarketSellRetry(String symbol, BigDecimal quantity) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", "SELL");
        params.put("type", "MARKET");
        params.put("quantity", quantity.toPlainString());

        String json = sendSigned(HttpMethod.POST, ORDER_EP, params);
        log.info("✅ MARKET SELL (retry) {} qty={}", symbol, quantity);
        return json;
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
        Map<String, String> q = new HashMap<>(params);
        q.put("timestamp", String.valueOf(getServerTime()));
        q.put("recvWindow", String.valueOf(DEFAULT_WINDOW));

        String query = q.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
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

    private static class LotRule {
        final BigDecimal stepSize;
        final BigDecimal minQty;
        LotRule(BigDecimal stepSize, BigDecimal minQty) {
            this.stepSize = stepSize;
            this.minQty = minQty;
        }
    }
    public String startUserDataStream() {
        requireKeys();
        return sendSigned(HttpMethod.POST, "/api/v3/userDataStream", Map.of());
    }

    public void keepAliveUserDataStream(String listenKey) {
        requireKeys();
        sendSigned(HttpMethod.PUT, "/api/v3/userDataStream", Map.of("listenKey", listenKey));
    }
    public String placeOcoSell(String symbol,
                               BigDecimal rawQty,
                               BigDecimal stopLossPrice,
                               BigDecimal takeProfitPrice) {
        requireKeys();
        ensureExchangeInfoLoaded();

        SymbolInfo info = exchangeInfo.getSymbols().stream()
                .filter(s -> s.getSymbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbol));

        SymbolFilter pf = info.getFilters().stream()
                .filter(f -> "PRICE_FILTER".equals(f.getFilterType()))
                .findFirst().orElseThrow();

        int priceScale = new BigDecimal(pf.getTickSize()).stripTrailingZeros().scale();

        BigDecimal quantity = normalizeQuantity(symbol, rawQty);
        if (quantity.signum() == 0) {
            throw new RuntimeException("Quantity too small for symbol " + symbol);
        }

        BigDecimal tick = new BigDecimal(pf.getTickSize());
        BigDecimal tp   = takeProfitPrice.setScale(priceScale, RoundingMode.DOWN);
        BigDecimal sp   = stopLossPrice .setScale(priceScale, RoundingMode.UP);
        BigDecimal last = getLastPrice(symbol);

        BigDecimal minTP = last.add(tick).setScale(priceScale, RoundingMode.DOWN);
        if (tp.compareTo(minTP) < 0) tp = minTP;
        BigDecimal maxSP = last.subtract(tick).setScale(priceScale, RoundingMode.UP);
        if (sp.compareTo(maxSP) > 0) sp = maxSP;
        BigDecimal slimit = sp.subtract(tick).setScale(priceScale, RoundingMode.DOWN);

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",              symbol);
        params.put("side",                "SELL");
        params.put("quantity",            quantity.toPlainString());
        params.put("price",               tp.toPlainString());
        params.put("stopPrice",           sp.toPlainString());
        params.put("stopLimitPrice",      slimit.toPlainString());
        params.put("stopLimitTimeInForce","GTC");

        try {
            String json = sendSigned(HttpMethod.POST, "/api/v3/order/oco", params);
            log.info("OCO SELL {} qty={} TP={} stopPrice={} stopLimit={}",
                    symbol, quantity, tp, sp, slimit);
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

    public Map<String, BalanceInfo> getFullBalance() {
        requireKeys();
        String json = sendSigned(HttpMethod.GET, ACCOUNT_EP, Collections.emptyMap());
        try {
            Map<String, BalanceInfo> result = new LinkedHashMap<>();
            JsonNode arr = objectMapper.readTree(json).get("balances");
            for (JsonNode b : arr) {
                BigDecimal free  = new BigDecimal(b.get("free").asText());
                BigDecimal locked = new BigDecimal(b.get("locked").asText());
                if (free.signum() > 0 || locked.signum() > 0) {
                    String asset = b.get("asset").asText();
                    result.put(asset, new BalanceInfo(asset, free, locked));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка getFullBalance", e);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class BalanceInfo {
        private final String asset;
        private final BigDecimal free;
        private final BigDecimal locked;

        public BigDecimal getTotal() {
            return free.add(locked);
        }
    }



}
