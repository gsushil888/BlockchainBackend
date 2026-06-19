package com.sushil.audittrail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "at_compliance_info")
public class ComplianceInfo {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String auditEventId;

    @ElementCollection
    @CollectionTable(name = "at_pii_categories", joinColumns = @JoinColumn(name = "compliance_id"))
    @Column(name = "category")
    private List<String> piiCategory;

    @Column(nullable = false)
    @Builder.Default
    private boolean piiAccessFlag = false;

    @Column(length = 200)
    private String consentTokenReference;

    @Column(length = 100)
    private String consentPurposeCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean externalLlmUsageFlag = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean customerAiDisclosureDeliveredFlag = false;

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
