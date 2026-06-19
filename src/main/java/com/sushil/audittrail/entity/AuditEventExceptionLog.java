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
@Table(name = "at_event_exception_log", indexes = {
        @Index(name = "idx_at_exc_type", columnList = "exceptionType"),
        @Index(name = "idx_at_exc_created", columnList = "createdAt")
})
public class AuditEventExceptionLog {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String exceptionType;   // FAILED_AI_RESPONSE | SYSTEM_EXCEPTION

    @Column(nullable = false, length = 200)
    private String errorCode;

    @Column(nullable = false, length = 1000)
    private String errorMessage;

    @Column(length = 100)
    private String modelId;

    @Column(length = 100)
    private String correlationId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum ExceptionType {
        FAILED_AI_RESPONSE, SYSTEM_EXCEPTION
    }
}
