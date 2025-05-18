// src/main/java/com/chicu/trader/trading/util/SignatureUtil.java
package com.chicu.trader.trading.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class SignatureUtil {

    /**
     * Собирает queryString из params, подписывает HMAC-SHA256 и возвращает полный строковый запрос.
     */
    public static String sign(Map<String,String> params, String secret) {
        String query = params.entrySet().stream()
            .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
            .collect(Collectors.joining("&"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(query.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return query + "&signature=" + sb;
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка при подписывании сообщения", ex);
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                         .replace("+", "%20")
                         .replace("*", "%2A")
                         .replace("%7E", "~");
    }
}
