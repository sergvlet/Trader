package com.chicu.trader.bot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "username")
    private String username;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "phone")
    private String phone;

    /**
     * По умолчанию false; @Builder.Default гарантирует,
     * что билдер установит false при отсутствии явного значения.
     */
    @Builder.Default
    @Column(name = "trading_enabled", nullable = false)
    private Boolean tradingEnabled = false;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserSettings settings;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @PrePersist
    private void prePersist() {
        if (registrationDate == null) {
            registrationDate = LocalDateTime.now();
        }
    }
}
