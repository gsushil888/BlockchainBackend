package com.sushil.audittrail.repository;

import com.sushil.audittrail.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String>,
        JpaSpecificationExecutor<AuditEvent> {

    Optional<AuditEvent> findByAuditEventId(String auditEventId);

    Optional<AuditEvent> findTopByOrderByCreatedAtDesc();

    long countByComplianceInfoPiiAccessFlagTrue();

    long countByComplianceInfoConsentTokenReferenceIsNull();

    long countByComplianceInfoExternalLlmUsageFlagTrue();

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.decisionType = :type")
    long countByDecisionType(@Param("type") String type);

    @Query("SELECT p, COUNT(c) FROM ComplianceInfo c JOIN c.piiCategory p GROUP BY p")
    List<Object[]> countByPiiCategory();

    // ── Dashboard ─────────────────────────────────────────────────────────────

    long countByTimestampAfter(LocalDateTime after);

    long countByComplianceInfoPiiAccessFlagTrueAndTimestampAfter(LocalDateTime after);

    // High-risk = confidenceScore < threshold OR decisionType in (DENY, ESCALATE)
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :after AND " +
           "(a.confidenceScore < :threshold OR a.decisionType IN ('DENY','ESCALATE','HIGH_RISK'))")
    long countHighRiskAfter(@Param("after") LocalDateTime after, @Param("threshold") double threshold);

    // Recent events page for the table (today / 7d / 30d driven by caller)
    Page<AuditEvent> findByTimestampAfterOrderByTimestampDesc(LocalDateTime after, Pageable pageable);

    // Decision breakdown for the given window
    @Query("SELECT a.decisionType, COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :after GROUP BY a.decisionType")
    List<Object[]> countGroupByDecisionTypeAfter(@Param("after") LocalDateTime after);

    // Model activity breakdown
    @Query("SELECT a.modelRegistry.modelId, COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :after GROUP BY a.modelRegistry.modelId")
    List<Object[]> countGroupByModelIdAfter(@Param("after") LocalDateTime after);

    // CSV export — all events in window, no pagination
    @Query("SELECT a FROM AuditEvent a WHERE a.timestamp >= :after ORDER BY a.timestamp DESC")
    List<AuditEvent> findAllForExport(@Param("after") LocalDateTime after);
}
