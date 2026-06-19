package com.sushil.audittrail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "at_actor_info")
public class ActorInfo {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String auditEventId;

    @Column(nullable = false, length = 100)
    private String initiatingUserId;

    @Column(nullable = false, length = 100)
    private String userRole;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(length = 100)
    private String cbsReferenceId;

    @Column(length = 64)
    private String clientIpHash;

    @Column(length = 200)
    private String sessionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
