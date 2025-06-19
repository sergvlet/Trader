package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.trading.entity.TradeLog;
import com.chicu.trader.trading.repository.TradeLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceUserDataStreamService extends TextWebSocketHandler {

    private final BinanceHttpClient  httpClient;
    private final ObjectMapper       objectMapper;
    private final TradeLogRepository tradeLogRepo;

    @Value("${binance.user-data-stream.keepalive-ms}")
    private String keepAliveMs;

    private WebSocketSession ws;
    private String           listenKey;

    @PostConstruct
    public void start() {
        try {
            // 1) Запуск User Data Stream
            String body = httpClient.startUserDataStream();
            JsonNode root = objectMapper.readTree(body);
            listenKey = root.path("listenKey").asText(null);
            if (listenKey == null) {
                log.warn("Не удалось получить listenKey — бин не запускаем");
                return;
            }

            // 2) Открываем WS-подключение
            String wsUrl = httpClient.getBaseUrl().replaceFirst("^http", "ws") + "/ws/" + listenKey;
            ws = new StandardWebSocketClient()
                    .doHandshake(this,
                            new WebSocketHttpHeaders(),
                            URI.create(wsUrl))
                    .get();

        } catch (IllegalStateException ise) {
            log.warn("Не запущен UserDataStream (нет ключей): {}", ise.getMessage());
        } catch (Exception ex) {
            log.error("Ошибка при инициализации UserDataStream", ex);
        }
    }

    /**
     * Периодическое продление listenKey.
     */
    @Scheduled(fixedDelayString = "${binance.user-data-stream.keepalive-ms}")
    public void keepAlive() {
        if (listenKey == null) {
            return;
        }
        try {
            httpClient.keepAliveUserDataStream(listenKey);
            log.debug("BinanceUserDataStreamService: продлён listenKey={}", listenKey);
        } catch (Exception ex) {
            log.warn("BinanceUserDataStreamService: не удалось продлить listenKey={}: {}",
                    listenKey, ex.getMessage());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());

            // нас интересуют только executionReport → FILLED → SELL
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

                        log.info("Trade #{} closed: exit={}, pnl={}",
                                entry.getId(), price, pnl);
                    });
        } catch (Exception ex) {
            log.error("BinanceUserDataStreamService: ошибка обработки WS-сообщения", ex);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        log.warn("BinanceUserDataStreamService: WebSocket закрыт: {}", status);
    }
}
