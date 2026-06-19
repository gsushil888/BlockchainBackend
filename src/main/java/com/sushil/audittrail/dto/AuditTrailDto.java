package com.sushil.audittrail.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AuditTrailDto {

    private AuditTrailDto() {}

    // ── Model Registry ────────────────────────────────────────────────────────

    public record CreateModelRequest(
            @NotBlank String modelId,
            @NotBlank String modelVersion,
            @NotBlank String modelType,
            @NotBlank String provider
    ) {}

    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ModelResponse {
        String id;
        String modelId;
        String modelVersion;
        String modelType;
        String provider;
        String status;
    }

    @Value @Builder
    public static class CreateModelResponse {
        String id;
        String status;
    }

    // ── Audit Event ───────────────────────────────────────────────────────────

    public record CreateAuditEventRequest(
            @NotBlank String modelId,
            @NotBlank String decisionType,
            String systemPrompt,
            @NotBlank String inputHash,
            @NotBlank String outputHash,
            @DecimalMin("0.0") @DecimalMax("1.0") double confidenceScore,
            @Min(0) long decisionLatencyMs,
            List<String> piiCategory,
            boolean piiAccessFlag,
            String consentTokenReference,
            String consentPurposeCode,
            boolean externalLlmUsageFlag,
            boolean customerAiDisclosureDeliveredFlag,
            @NotBlank String initiatingUserId,
            @NotBlank String userRole,
            @NotBlank String channel,
            String cbsReferenceId,
            String clientIpHash,
            String sessionId
    ) {}

    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateAuditEventResponse {
        String id;
        String auditEventId;
        String status;
        String message;
    }

    // Search/list row — lean
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuditEventResponse {
        String auditEventId;
        String decisionType;
        long timestamp;             // epoch millis
        String date;                // ISO-8601 e.g. 2026-06-18T16:11:52
        String modelId;
        double confidenceScore;
        String riskLevel;           // CRITICAL | HIGH | MEDIUM | LOW
        String correlationId;
        String inputHash;
        String outputHash;
        long decisionLatencyMs;
        String currentEntryHash;
        String previousEntryHash;
    }

    // Full detail view
    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuditEventDetailResponse {
        // Core Identifiers & Traceability
        String auditEventId;
        long timestamp;             // epoch millis
        String correlationId;
        String previousEntryHash;
        String currentEntryHash;
        String riskLevel;

        // User & Interaction Context
        String initiatingUserId;
        String userRole;
        String channel;
        String cbsReferenceId;
        String sessionId;
        String clientIpHash;

        // Model Governance
        String modelId;
        String modelVersion;
        String provider;
        String modelType;
        String systemPrompt;

        // Security & Privacy
        List<String> piiCategory;
        boolean piiAccessFlag;
        String consentTokenReference;
        String consentPurposeCode;
        boolean externalLlmUsageFlag;
        boolean customerAiDisclosureDeliveredFlag;

        // Decision Metrics
        String decisionType;
        double confidenceScore;
        long decisionLatencyMs;

        // Input / Output
        String inputHash;
        String outputHash;
    }

    @Value @Builder
    public static class ChainVerifyResponse {
        String auditEventId;
        boolean chainValid;
        boolean currentHashVerified;
        boolean previousHashVerified;
    }

    // ── Compliance ────────────────────────────────────────────────────────────

    @Value @Builder
    public static class ComplianceSummaryResponse {
        long totalAuditRecords;
        long piiEvents;
        long missingConsent;
        long externalLlmUsage;
    }

    @Value @Builder
    public static class ConsentReportResponse {
        long total;
        long validConsent;
        long missingConsent;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public enum DashboardWindow { TODAY, LAST_7_DAYS, LAST_30_DAYS }

    @Value @Builder
    public static class DashboardSummaryResponse {
        long totalAuditEvents;
        long highRiskDecisions;
        long piiAccessEvents;
        long failedAiResponses;
        long systemExceptions;
        Map<String, Long> decisionBreakdown;
        Map<String, Long> modelActivity;
    }

    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DashboardAuditEventRow {
        String auditEventId;
        long timestamp;             // epoch millis
        String date;                // ISO-8601 e.g. 2026-06-18T16:11:52
        String modelId;
        double confidenceScore;
        long decisionLatencyMs;
        boolean piiAccessFlag;
        String riskLevel;           // CRITICAL | HIGH | MEDIUM | LOW
    }

    @Value @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExceptionLogRow {
        String id;
        String exceptionType;
        String errorCode;
        String errorMessage;
        String modelId;
        String correlationId;
        LocalDateTime createdAt;
    }
}
