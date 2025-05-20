package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.Candle;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpBinanceWebSocketService {

    private static final String PROD_WS = "wss://stream.binance.com:9443/ws";
    private static final String TEST_WS = "wss://testnet.binance.vision/ws";

    private final AiTradingSettingsService settingsService;
    private final HttpBinanceCandleService  candleService;
    private final ObjectMapper              objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // Подписываемся на обновления для каждого пользователя и его списка пар
        HttpClient client = HttpClient.newHttpClient();
        List<Long> chatIds = settingsService.findAllChatIds();
        for (Long chatId : chatIds) {
            var settings = settingsService.getOrCreate(chatId);
            String base = "test".equalsIgnoreCase(settings.getNetworkMode()) ? TEST_WS : PROD_WS;
            List<String> symbols = settings.getSymbols() == null || settings.getSymbols().isBlank()
                    ? List.of()
                    : List.of(settings.getSymbols().split(","));
            for (String symbol : symbols) {
                String uri = String.format("%s/%s@kline_%s", base, symbol.toLowerCase(), settings.getTimeframe());
                client.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .buildAsync(URI.create(uri), new WebSocket.Listener() {
                            private final StringBuilder sb = new StringBuilder();
                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                sb.append(data);
                                if (!last) return WebSocket.Listener.super.onText(webSocket, data, last);
                                String msg = sb.toString();
                                sb.setLength(0);
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String,Object> m = objectMapper.readValue(msg, Map.class);
                                    @SuppressWarnings("unchecked")
                                    Map<String,Object> k = (Map<String,Object>) m.get("k");
                                    Candle c = new Candle(
                                            symbol.toUpperCase(),
                                            ((Number) k.get("t")).longValue(),
                                            Double.parseDouble(k.get("o").toString()),
                                            Double.parseDouble(k.get("h").toString()),
                                            Double.parseDouble(k.get("l").toString()),
                                            Double.parseDouble(k.get("c").toString()),
                                            Double.parseDouble(k.get("v").toString()),
                                            ((Number) k.get("T")).longValue()
                                    );
                                    // Передаём chatId и новую свечу в сервис
                                    candleService.onWebSocketCandleUpdate(c);
                                } catch (Exception ex) {
                                    log.error("Ошибка парсинга WS-сообщения для {}: {}", uri, ex.getMessage());
                                }
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }
                            @Override public void onOpen(WebSocket webSocket) {
                                log.info("WS подключено к {}", uri);
                            }
                            @Override public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                                return WebSocket.Listener.super.onBinary(webSocket, data, last);
                            }
                            @Override public void onError(WebSocket webSocket, Throwable error) {
                                log.error("Ошибка WS для " + uri, error);
                            }
                        });
            }
        }
        log.info("WebSocket подписки установлены для {} пользователей", chatIds.size());
    }
}
