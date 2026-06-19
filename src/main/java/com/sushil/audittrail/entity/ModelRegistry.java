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
@Table(name = "at_model_registry", indexes = {
        @Index(name = "idx_at_model_id", columnList = "modelId"),
        @Index(name = "idx_at_model_provider", columnList = "provider")
})
public class ModelRegistry {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String modelId;

    @Column(nullable = false, length = 100)
    private String modelVersion;

    @Column(nullable = false, length = 50)
    private String modelType;

    @Column(nullable = false, length = 100)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ModelStatus status = ModelStatus.ACTIVE;

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

    public enum ModelStatus { ACTIVE, INACTIVE }
}
