package com.sushil.audittrail.service;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.entity.AuditEvent;
import com.sushil.audittrail.entity.AuditEventExceptionLog;
import com.sushil.audittrail.repository.AuditEventExceptionLogRepository;
import com.sushil.audittrail.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // Confidence below this counts as high-risk for the summary KPI card
    private static final double HIGH_RISK_THRESHOLD = 0.7;

    private final AuditEventRepository auditRepo;
    private final AuditEventExceptionLogRepository exceptionRepo;

    // ── Summary card counts ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(DashboardWindow window) {
        LocalDateTime from = windowStart(window);

        long total      = auditRepo.countByTimestampAfter(from);
        long highRisk   = auditRepo.countHighRiskAfter(from, HIGH_RISK_THRESHOLD);
        long piiAccess  = auditRepo.countByComplianceInfoPiiAccessFlagTrueAndTimestampAfter(from);
        long failedAi   = exceptionRepo.countByExceptionTypeAndCreatedAtAfter(
                              AuditEventExceptionLog.ExceptionType.FAILED_AI_RESPONSE.name(), from);
        long exceptions = exceptionRepo.countByExceptionTypeAndCreatedAtAfter(
                              AuditEventExceptionLog.ExceptionType.SYSTEM_EXCEPTION.name(), from);

        Map<String, Long> decisionBreakdown = auditRepo.countGroupByDecisionTypeAfter(from).stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

        Map<String, Long> modelActivity = auditRepo.countGroupByModelIdAfter(from).stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

        return DashboardSummaryResponse.builder()
                .totalAuditEvents(total)
                .highRiskDecisions(highRisk)
                .piiAccessEvents(piiAccess)
                .failedAiResponses(failedAi)
                .systemExceptions(exceptions)
                .decisionBreakdown(decisionBreakdown)
                .modelActivity(modelActivity)
                .build();
    }

    // ── Audit events table ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DashboardAuditEventRow> auditEventTable(DashboardWindow window, Pageable pageable) {
        return auditRepo.findByTimestampAfterOrderByTimestampDesc(windowStart(window), pageable)
                .map(this::toRow);
    }

    // ── Exception / failed AI log ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ExceptionLogRow> exceptionLog(DashboardWindow window, Pageable pageable) {
        return exceptionRepo.findRecentExceptions(windowStart(window), pageable)
                .map(this::toExceptionRow);
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportCsv(DashboardWindow window) {
        List<AuditEvent> events = auditRepo.findAllForExport(windowStart(window));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter w = new PrintWriter(out)) {
            w.println("auditEventId,decisionType,timestamp,modelId,confidenceScore," +
                      "riskLevel,channel,initiatingUserId,piiAccessFlag,inputHash,outputHash");
            for (AuditEvent e : events) {
                w.printf("%s,%s,%d,%s,%.4f,%s,%s,%s,%b,%s,%s%n",
                        e.getAuditEventId(),
                        e.getDecisionType(),
                        e.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli(),
                        e.getModelRegistry().getModelId(),
                        e.getConfidenceScore(),
                        AuditEventService.resolveRiskLevel(e.getConfidenceScore(), e.getDecisionType()),
                        e.getActorInfo().getChannel(),
                        e.getActorInfo().getInitiatingUserId(),
                        e.getComplianceInfo().isPiiAccessFlag(),
                        e.getInputHash(),
                        e.getOutputHash());
            }
        }
        return out.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDateTime windowStart(DashboardWindow window) {
        return switch (window) {
            case TODAY        -> LocalDateTime.now().toLocalDate().atStartOfDay();
            case LAST_7_DAYS  -> LocalDateTime.now().minusDays(7);
            case LAST_30_DAYS -> LocalDateTime.now().minusDays(30);
        };
    }

    private DashboardAuditEventRow toRow(AuditEvent e) {
        return DashboardAuditEventRow.builder()
                .auditEventId(e.getAuditEventId())
                .timestamp(e.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                .date(e.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                .modelId(e.getModelRegistry().getModelId())
                .confidenceScore(e.getConfidenceScore())
                .decisionLatencyMs(e.getDecisionLatencyMs())
                .piiAccessFlag(e.getComplianceInfo().isPiiAccessFlag())
                .riskLevel(AuditEventService.resolveRiskLevel(e.getConfidenceScore(), e.getDecisionType()))
                .build();
    }

    private ExceptionLogRow toExceptionRow(AuditEventExceptionLog e) {
        return ExceptionLogRow.builder()
                .id(e.getId())
                .exceptionType(e.getExceptionType())
                .errorCode(e.getErrorCode())
                .errorMessage(e.getErrorMessage())
                .modelId(e.getModelId())
                .correlationId(e.getCorrelationId())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
