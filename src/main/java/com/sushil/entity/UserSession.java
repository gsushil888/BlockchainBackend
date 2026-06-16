package com.sushil.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_sessions_token",   columnList = "token"),
        @Index(name = "idx_sessions_user_id", columnList = "user_id")
})
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /** LOGIN_PASSWORD_USERNAME | LOGIN_PASSWORD_EMAIL | LOGIN_MOBILE_OTP */
    @Column(length = 30)
    private String loginMethod;

    @Column(length = 45)
    private String loginIp;

    @Column(length = 256)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
