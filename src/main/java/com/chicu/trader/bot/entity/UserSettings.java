// src/main/java/com/chicu/trader/bot/entity/UserSettings.java
package com.chicu.trader.bot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {
    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne
    @MapsId
    @JoinColumn(name = "chat_id")
    private User user;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "mode")
    private String mode;

    @Column(name = "test_api_key")
    private String testApiKey;

    @Column(name = "test_secret_key")
    private String testSecretKey;

    @Column(name = "real_api_key")
    private String realApiKey;

    @Column(name = "real_secret_key")
    private String realSecretKey;

    private Double maxEquity;
    private Long nextAllowedTradeTime;

    public boolean hasTestCredentials() {
        return testApiKey != null && testSecretKey != null;
    }
    public boolean hasRealCredentials() {
        return realApiKey != null && realSecretKey != null;
    }
    public boolean hasCredentialsFor(String mode) {
        if ("REAL".equalsIgnoreCase(mode)) return hasRealCredentials();
        else return hasTestCredentials();
    }
}
