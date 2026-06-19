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
@Table(name = "at_audit_event", indexes = {
        @Index(name = "idx_at_ae_audit_event_id", columnList = "auditEventId"),
        @Index(name = "idx_at_ae_correlation_id", columnList = "correlationId"),
        @Index(name = "idx_at_ae_decision_type", columnList = "decisionType"),
        @Index(name = "idx_at_ae_timestamp", columnList = "timestamp")
})
public class AuditEvent {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String auditEventId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 100)
    private String correlationId;

    @Column(nullable = false, length = 50)
    private String decisionType;

    @Column(length = 1000)
    private String systemPrompt;

    @Column(nullable = false, length = 64)
    private String inputHash;

    @Column(nullable = false, length = 64)
    private String outputHash;

    @Column(nullable = false)
    private double confidenceScore;

    @Column(nullable = false)
    private long decisionLatencyMs;

    @Column(nullable = false, length = 64)
    private String currentEntryHash;

    @Column(nullable = false, length = 64)
    private String previousEntryHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_registry_id", nullable = false)
    private ModelRegistry modelRegistry;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compliance_info_id", nullable = false)
    private ComplianceInfo complianceInfo;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_info_id", nullable = false)
    private ActorInfo actorInfo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = updatedAt = LocalDateTime.now();
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
