package com.chicu.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class TraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraderApplication.class, args);
    }

}
