package com.chicu.trader.trading.service.binance;

import com.chicu.trader.trading.service.binance.client.BinanceRestClient;
import com.chicu.trader.trading.service.binance.client.BinanceRestClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final BinanceRestClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String placeMarketBuy(Long chatId, String symbol, BigDecimal qty) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        String raw = client.placeMarketBuyRaw(symbol, qty);
        JsonNode n = objectMapper.readTree(raw);
        return n.get("clientOrderId").asText();
    }

    @SneakyThrows
    public String placeMarketSell(Long chatId, String symbol, BigDecimal qty) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        String raw = client.placeMarketSellRaw(symbol, qty);
        JsonNode n = objectMapper.readTree(raw);
        return n.get("clientOrderId").asText();
    }

    @SneakyThrows
    public String placeOcoSell(Long chatId,
                               String symbol,
                               BigDecimal qty,
                               BigDecimal stopLossPrice,
                               BigDecimal takeProfitPrice) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        String raw;
        try {
            // Попытка OCO
            raw = client.placeOcoSellRaw(symbol, qty, stopLossPrice, takeProfitPrice);
        } catch (HttpClientErrorException.BadRequest ex) {
            String body = ex.getResponseBodyAsString();
            if (body.contains("\"code\":-2010")) {
                // Фильтр не прошел — делаем MARKET SELL
                raw = client.placeMarketSellRaw(symbol, qty);
            } else {
                throw ex;
            }
        }
        JsonNode root = objectMapper.readTree(raw);
        // Если OCO — ищем массив orderReports
        JsonNode reports = root.get("orderReports");
        if (reports != null && reports.isArray() && reports.size() > 0) {
            return reports.get(0).get("clientOrderId").asText();
        }
        // Иначе — это MARKET SELL, берём clientOrderId из корня
        if (root.has("clientOrderId")) {
            return root.get("clientOrderId").asText();
        }
        throw new IllegalStateException("Не удалось извлечь clientOrderId из ответа: " + raw);
    }
    /** Получить статус OCO-ордера по его listId */
    @SneakyThrows
    public JsonNode getOcoStatus(Long chatId, String symbol, String listId) {
        BinanceRestClient client = clientFactory.getClient(chatId);
        // /api/v3/orderList?listId={listId}&symbol={symbol}
        String raw = client.getOcoStatusRaw(symbol, listId);
        return objectMapper.readTree(raw);
    }
}
