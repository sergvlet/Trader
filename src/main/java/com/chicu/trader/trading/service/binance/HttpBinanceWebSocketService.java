// src/main/java/com/chicu/trader/trading/service/binance/HttpBinanceWebSocketService.java
package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.Candle;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpBinanceWebSocketService {

    private static final String PROD_WS = "wss://stream.binance.com:9443/ws";
    private static final String TEST_WS = "wss://testnet.binance.vision/ws";

    private final AiTradingSettingsService settingsService;
    private final HttpBinanceCandleService candleService;
    private final ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<String, WebSocket> sockets = new ConcurrentHashMap<>();

    /**
     * Запустить подписки по списку символов.
     * Для каждого символа будет вызываться candleService.onWebSocketCandleUpdate(c).
     */
    public synchronized void startSubscriptions(Long chatId, List<String> symbols) {
        startSubscriptions(chatId, symbols, candleService::onWebSocketCandleUpdate);
    }

    /**
     * Запустить подписки по списку символов с пользовательским колбэком onCandle.
     *
     * @param chatId   чей сеттинг брать для выбора PROD_WS / TEST_WS
     * @param symbols  список символов (например, ["BTCUSDT","ETHUSDT"])
     * @param onCandle колбэк, вызываемый для каждого закрытого бара
     */
    public synchronized void startSubscriptions(
            Long chatId,
            List<String> symbols,
            Consumer<Candle> onCandle
    ) {
        // Закрываем предыдущие WS
        stopSubscriptions();

        String mode = settingsService.getOrCreate(chatId).getNetworkMode();
        String base = "test".equalsIgnoreCase(mode) ? TEST_WS : PROD_WS;

        for (String sym : symbols) {
            String stream = sym.toLowerCase() + "@kline_1m";
            String uri = base + "/" + stream;
            log.info("WS connecting to {}", uri);

            client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create(uri), new WebSocket.Listener() {
                        private final StringBuilder sb = new StringBuilder();

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            sb.append(data);
                            if (!last) {
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }
                            String msg = sb.toString();
                            sb.setLength(0);

                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = objectMapper.readValue(msg, Map.class);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> k = (Map<String, Object>) m.get("k");

                                // Флаг завершения бара
                                Boolean isFinal = (Boolean) k.get("x");
                                if (Boolean.TRUE.equals(isFinal)) {
                                    Candle c = new Candle(
                                            (String) k.get("s"),
                                            ((Number) k.get("t")).longValue(),
                                            Double.parseDouble(k.get("o").toString()),
                                            Double.parseDouble(k.get("h").toString()),
                                            Double.parseDouble(k.get("l").toString()),
                                            Double.parseDouble(k.get("c").toString()),
                                            Double.parseDouble(k.get("v").toString()),
                                            ((Number) k.get("T")).longValue()
                                    );
                                    onCandle.accept(c);
                                }
                            } catch (Exception ex) {
                                log.error("WS parse error for {}: {}", sym, ex.getMessage());
                            }
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            log.info("WS opened {}", uri);
                            sockets.put(sym, webSocket);
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                            return WebSocket.Listener.super.onBinary(webSocket, data, last);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            log.error("WS error on {}: {}", uri, error.getMessage());
                            WebSocket.Listener.super.onError(webSocket, error);
                        }
                    });
        }
    }

    /**
     * Отключить все подписки.
     */
    public synchronized void stopSubscriptions() {
        sockets.forEach((sym, ws) -> {
            log.info("WS closing for {}", sym);
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "stop").join();
            } catch (Exception e) {
                log.warn("Error closing WS for {}: {}", sym, e.getMessage());
            }
        });
        sockets.clear();
    }
}
