// src/main/java/com/chicu/trader/config/AsyncConfig.java
package com.chicu.trader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "optimizerExecutor")
    public Executor optimizerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Подберите под себя:
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("optimizer-");
        executor.initialize();
        return executor;
    }

    // При желании можно оставить дефолтный executor под @Async без параметров
    // или задать и другие bean’ы с @Bean(name="...") для разных задач.
}
