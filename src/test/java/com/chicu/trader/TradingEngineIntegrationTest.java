//// src/test/java/com/chicu/trader/TradingEngineIntegrationTest.java
//package com.chicu.trader;
//
//import com.chicu.trader.bot.entity.UserSettings;
//import com.chicu.trader.bot.menu.core.MenuService;
//import com.chicu.trader.bot.repository.UserSettingsRepository;
//import com.chicu.trader.bot.service.MenuSessionService;
//import com.chicu.trader.bot.service.TelegramSender;
//import com.chicu.trader.model.ProfitablePair;
//import com.chicu.trader.model.TradeLog;
//import com.chicu.trader.repository.ProfitablePairRepository;
//import com.chicu.trader.repository.TradeLogRepository;
//import com.chicu.trader.trading.*;
//import com.chicu.trader.trading.context.StrategyContext;
//import com.chicu.trader.trading.model.Candle;
//import com.chicu.trader.trading.ml.MlModelTrainer;
//import com.chicu.trader.trading.optimizer.TpSlOptimizer;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mockito;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.system.CapturedOutput;
//import org.springframework.boot.test.system.OutputCaptureExtension;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Primary;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Flux;
//import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.BDDMockito.given;
//
//@ExtendWith(OutputCaptureExtension.class)
//@SpringBootTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@Slf4j
//class TradingEngineIntegrationTest {
//
//    @MockBean
//    private UserSettingsRepository userSettingsRepository;
//
//    @TestConfiguration
//    static class TestBeans {
//        @Bean @Primary
//        MarketDataService marketDataService(WebClient.Builder b) {
//            return new MarketDataService(b) {
//                @Override public List<String> getTopNLiquidPairs(int n) {
//                    return List.of("BTCUSDT");
//                }
//            };
//        }
//
//        @Bean @Primary
//        CandleService candleService() {
//            return new InMemoryCandleService();
//        }
//
//        @Bean @Primary
//        MlTrainer mlTrainerStub(
//                CandleService cs,
//                ProfitablePairRepository pr,
//                TpSlOptimizer opt,
//                MlModelTrainer mt
//        ) {
//            return new MlTrainer(cs, pr, opt, mt) {
//                @Override public void trainNow(Long chatId) {
//                    log.info("Model retraining stub for {}", chatId);
//                }
//            };
//        }
//
//        @Bean @Primary
//        public BalanceService balanceService() {
//            return new BalanceService() {
//                @Override public double getAvailableBalance(Long chatId) {
//                    log.info("BalanceService.getAvailableBalance for {}", chatId);
//                    return 1_000_000;
//                }
//            };
//        }
//
//        @Bean @Primary
//        public RiskManager riskManager(BalanceService bs, UserSettingsRepository us) {
//            return new RiskManager(bs, us) {
//                @Override
//                public boolean allowNewTrades(Long chatId) {
//                    log.info("RiskManager.allow for {}", chatId);
//                    return true;
//                }
//            };
//        }
//
//        @Bean @Primary
//        StrategyFacade strategyFacade() {
//            return new StrategyFacade() {
//                @Override
//                public StrategyContext buildContext(Long chatId, Candle candle, List<ProfitablePair> pairs) {
//                    log.info("Building context for {} {}", chatId, candle.getSymbol());
//                    return new StrategyContext(chatId, candle.getSymbol(), candle.getClose(), candle);
//                }
//                @Override public boolean shouldEnter(StrategyContext ctx) {
//                    log.info("Attempting entry for {}", ctx.getSymbol());
//                    return true;
//                }
//            };
//        }
//
//        @Bean @Primary
//        OrderService orderService() {
//            return new OrderService() {
//                @Override
//                public TradeLog openPosition(StrategyContext ctx) {
//                    log.info("Entry executed for {}", ctx.getChatId());
//                    TradeLog tl = new TradeLog();
//                    tl.setUserChatId(ctx.getChatId());
//                    tl.setSymbol(ctx.getSymbol());
//                    tl.setEntryPrice(ctx.getPrice());
//                    return tl;
//                }
//                @Override public Optional<TradeLog> checkAndClose(StrategyContext ctx) {
//                    return Optional.empty();
//                }
//            };
//        }
//
//        @Bean @Primary
//        public TradeLogRepository tradeLogRepository() {
//            return new TradeLogRepository() {
//                private final List<TradeLog> store = new ArrayList<>();
//                @Override public <S extends TradeLog> S save(S entity) {
//                    store.add(entity);
//                    return entity;
//                }
//                // остальные методы — throw new UnsupportedOperationException()
//            };
//        }
//
//        @Bean @Primary
//        public TelegramSender telegramSender() {
//            return new TelegramSender(null) {
//                @Override public void executeEdit(EditMessageText e) { /* no-op */ }
//                @Override public void sendText(Long c, String t)    { /* no-op */ }
//            };
//        }
//
//        static class InMemoryCandleService implements CandleService {
//            private final Map<Long,List<Candle>> map = new ConcurrentHashMap<>();
//
//            @Override
//            public Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs) {
//                return Flux.defer(() ->
//                        Flux.fromIterable(map.getOrDefault(chatId, Collections.emptyList()))
//                                .repeat()
//                                .delayElements(Duration.ofMillis(100))
//                );
//            }
//            @Override public List<Candle> historyHourly(Long c, String s, int cnt){return List.of();}
//            @Override public List<Candle> history4h(Long c, String s, int cnt){return List.of();}
//
//            public void setStreamOverride(Long chatId, List<Candle> candles) {
//                map.put(chatId, candles);
//            }
//        }
//    }
//
//    @Autowired ProfitablePairRepository pairRepo;
//    @Autowired TradingEngine        engine;
//    @Autowired TradingStatusService status;
//    @Autowired CandleService        candles;
//    @Autowired MenuService          menu;
//    @Autowired MenuSessionService   session;
//
//    private static final Long CHAT_ID = 12345L;
//
//    @BeforeEach
//    void init() {
//        // настроим мок UserSettingsRepository
//        given(userSettingsRepository.findByChatId(anyLong()))
//                .willReturn(Optional.of(new UserSettings(CHAT_ID, /* любые дефолтные */)));
//
//        pairRepo.save(ProfitablePair.builder()
//                .userChatId(CHAT_ID)
//                .symbol("BTCUSDT")
//                .takeProfitPct(0.03)
//                .stopLossPct(0.01)
//                .active(true)
//                .build()
//        );
//        session.createMenuMessage(CHAT_ID, 1);
//    }
//
//    @Test
//    void fullLifecycleTest(CapturedOutput output) throws InterruptedException {
//        // 1) Старт
//        engine.startAutoTrading(CHAT_ID);
//        assertThat(status.isRunning(CHAT_ID)).isTrue();
//        assertThat(status.getLastEvent(CHAT_ID)).contains("▶️ Торговля запущена");
//        assertThat(output.getOut()).contains("Auto-trading started for " + CHAT_ID);
//
//        // 2) Эмуляция свечи
//        Candle fake = Candle.builder()
//                .symbol("BTCUSDT")
//                .openTime(Instant.now().toEpochMilli())
//                .open(100).high(105).low(99).close(102).volume(1000)
//                .closeTime(Instant.now().plusSeconds(3600).toEpochMilli())
//                .build();
//        ((TestBeans.InMemoryCandleService)candles)
//                .setStreamOverride(CHAT_ID, List.of(fake));
//        Thread.sleep(500);
//
//        assertThat(status.getLastEvent(CHAT_ID)).contains("✅ Вход BTCUSDT");
//        assertThat(output.getOut())
//                .contains("Attempting entry for BTCUSDT")
//                .contains("Entry executed for " + CHAT_ID);
//
//        // 3) Стоп
//        engine.stopAutoTrading(CHAT_ID);
//        assertThat(status.isRunning(CHAT_ID)).isFalse();
//        assertThat(status.getLastEvent(CHAT_ID)).contains("⏹️ Торговля остановлена");
//        assertThat(output.getOut()).contains("Auto-trading stopped for " + CHAT_ID);
//
//        // для наглядности
//        System.out.println("\n=== Логи теста ===\n" + output.getOut());
//    }
//}
