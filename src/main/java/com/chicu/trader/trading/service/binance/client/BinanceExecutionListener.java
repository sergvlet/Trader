package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceExecutionListener extends TextWebSocketHandler {

    private final BinanceHttpClient  httpClient;
    private final ObjectMapper       objectMapper;
    private final TradeLogRepository tradeLogRepo;

    private WebSocketSession ws;
    private String           listenKey;

    @PostConstruct
    @SneakyThrows
    public void init() {
        try {
            // 1) Запрос нового listenKey
            String body = httpClient.startUserDataStream();
            JsonNode node = objectMapper.readTree(body);
            listenKey = node.path("listenKey").asText(null);
            if (listenKey == null) {
                log.warn("BinanceExecutionListener: не удалось получить listenKey из ответа");
                return;
            }
            log.info("BinanceExecutionListener: получен listenKey={}", listenKey);

            // 2) WS URL (http → ws)
            String wsBase = httpClient.getBaseUrl().replaceFirst("^http", "ws");
            String wsUrl  = wsBase + "/ws/" + listenKey;

            // 3) Открываем WebSocket по URL
            ws = new StandardWebSocketClient()
                    .doHandshake(this, wsUrl)
                    .get();

            // 4) Keep-alive каждые 30 минут
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(this::keepAlive, 30, 30, TimeUnit.MINUTES);

            log.info("BinanceExecutionListener: WebSocket подключён и KeepAlive запланирован");
        } catch (IllegalStateException ise) {
            // это значит, что не заданы API-ключи/секрет
            log.warn("BinanceExecutionListener не запущен (отсутствуют ключи): {}", ise.getMessage());
        } catch (Exception ex) {
            log.error("BinanceExecutionListener: ошибка при инициализации", ex);
        }
    }

    @SneakyThrows
    private void keepAlive() {
        try {
            httpClient.keepAliveUserDataStream(listenKey);
            log.debug("BinanceExecutionListener: отправлен KeepAlive для listenKey={}", listenKey);
        } catch (Exception ex) {
            log.warn("BinanceExecutionListener: не удалось продлить listenKey={}: {}", listenKey, ex.getMessage());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());

        // Нас интересуют только executionReport, FILLED и SELL
        if (!"executionReport".equals(root.path("e").asText())) return;
        if (!"FILLED".equals(root.path("X").asText()))      return;
        if (!"SELL".equals(root.path("S").asText()))        return;

        String clientId = root.path("c").asText();
        BigDecimal price = new BigDecimal(root.path("p").asText());
        Instant   time  = Instant.ofEpochMilli(root.path("E").asLong());

        tradeLogRepo.findOpenByEntryClientOrderId(clientId)
                .ifPresent(entry -> {
                    BigDecimal pnl = price
                            .subtract(entry.getEntryPrice())
                            .multiply(entry.getQuantity());

                    entry.setExitPrice(price);
                    entry.setExitTime(time);
                    entry.setClosed(true);
                    entry.setPnl(pnl);
                    tradeLogRepo.save(entry);

                    log.info("Trade #{} closed автоматически: exit={}, pnl={}", entry.getId(), price, pnl);
                });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        log.warn("BinanceExecutionListener: WebSocket закрыт: {}", status);
    }
}
