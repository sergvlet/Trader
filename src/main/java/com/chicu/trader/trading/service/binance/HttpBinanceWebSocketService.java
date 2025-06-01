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
import java.util.Map;
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
    private final Map<Long, Map<String, WebSocket>> userSockets = new ConcurrentHashMap<>();

    /**
     * Запускает подписки на свечи для заданных символов, используя стандартный обработчик onWebSocketCandleUpdate.
     */
    public synchronized void startSubscriptions(Long chatId, java.util.List<String> symbols) {
        startSubscriptions(chatId, symbols, candleService::onWebSocketCandleUpdate);
    }

    /**
     * Запускает подписки на свечи для заданных символов с кастомным Consumer<Candle>.
     */
    public synchronized void startSubscriptions(
            Long chatId,
            java.util.List<String> symbols,
            Consumer<Candle> onCandle
    ) {
        // Сначала прекращаем старые подписки
        stopSubscriptions(chatId);

        // Определяем базовый URL (прод/тест) и таймфрейм из настроек пользователя
        String mode = settingsService.getOrCreate(chatId).getNetworkMode();
        String base = "test".equalsIgnoreCase(mode) ? TEST_WS : PROD_WS;
        String timeframe = settingsService.getOrCreate(chatId).getTimeframe();
        if (timeframe == null || timeframe.isEmpty()) {
            timeframe = "1m";
            log.warn("Для chatId={} не задан timeframe, используем дефолт '{}'", chatId, timeframe);
        }

        log.info("Запуск WS-подписок для chatId={} с таймфреймом='{}'", chatId, timeframe);

        for (String sym : symbols) {
            // Формируем имя потока с учётом текущего таймфрейма
            String stream = sym.toLowerCase() + "@kline_" + timeframe;
            String uri = base + "/" + stream;
            log.info("Подключаюсь к WS: {}", uri);

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
                                log.error("Ошибка разбора WS-сообщения для символа '{}': {}", sym, ex.getMessage());
                            }
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            log.info("WS открыт: {}", uri);
                            userSockets
                                    .computeIfAbsent(chatId, id -> new ConcurrentHashMap<>())
                                    .put(sym, webSocket);
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                            return WebSocket.Listener.super.onBinary(webSocket, data, last);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            log.error("Ошибка WS для '{}': {}", uri, error.getMessage());
                            WebSocket.Listener.super.onError(webSocket, error);
                        }
                    });
        }
    }

    /**
     * Останавливает подписки WS для всех пользователей.
     */
    public synchronized void stopSubscriptions() {
        userSockets.forEach((chatId, map) -> stopSubscriptions(chatId));
    }

    /**
     * Останавливает подписки WS для конкретного пользователя (chatId).
     */
    public synchronized void stopSubscriptions(Long chatId) {
        Map<String, WebSocket> sockets = userSockets.remove(chatId);
        if (sockets != null) {
            sockets.forEach((sym, ws) -> {
                log.info("Закрываю WS для chatId={} символ={}", chatId, sym);
                try {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "stop").join();
                } catch (Exception e) {
                    log.warn("Ошибка при закрытии WS для '{}': {}", sym, e.getMessage());
                }
            });
        }
    }
}
