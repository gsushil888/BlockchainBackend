package com.sushil.audittrail.service;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.entity.ActorInfo;
import com.sushil.audittrail.entity.AuditEvent;
import com.sushil.audittrail.entity.ComplianceInfo;
import com.sushil.audittrail.entity.ModelRegistry;
import com.sushil.audittrail.repository.AuditEventRepository;
import com.sushil.audittrail.repository.AuditEventSpec;
import com.sushil.audittrail.repository.ModelRegistryRepository;
import com.sushil.exception.AppExceptions.BadRequestException;
import com.sushil.exception.AppExceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AuditEventRepository auditRepo;
    private final ModelRegistryRepository modelRepo;

    // Sequential counter — sufficient for MVP single-instance monolith
    private final AtomicLong sequence = new AtomicLong(0);

    @Transactional
    public CreateAuditEventResponse create(CreateAuditEventRequest req) {
        ModelRegistry model = modelRepo.findByModelId(req.modelId())
                .orElseThrow(() -> new BadRequestException("Unknown modelId: " + req.modelId()));

        String previousHash = auditRepo.findTopByOrderByCreatedAtDesc()
                .map(AuditEvent::getCurrentEntryHash)
                .orElse(GENESIS_HASH);

        String auditEventId = generateAuditEventId();

        String currentHash = sha256(
                auditEventId + req.inputHash() + req.outputHash() +
                req.decisionType() + req.initiatingUserId() + previousHash
        );

        ComplianceInfo compliance = ComplianceInfo.builder()
                .auditEventId(auditEventId)
                .piiCategory(req.piiCategory() != null ? req.piiCategory() : List.of())
                .piiAccessFlag(req.piiAccessFlag())
                .consentTokenReference(req.consentTokenReference())
                .consentPurposeCode(req.consentPurposeCode())
                .externalLlmUsageFlag(req.externalLlmUsageFlag())
                .customerAiDisclosureDeliveredFlag(req.customerAiDisclosureDeliveredFlag())
                .build();

        ActorInfo actor = ActorInfo.builder()
                .auditEventId(auditEventId)
                .initiatingUserId(req.initiatingUserId())
                .userRole(req.userRole())
                .channel(req.channel())
                .cbsReferenceId(req.cbsReferenceId())
                .clientIpHash(req.clientIpHash())
                .sessionId(req.sessionId())
                .build();

        AuditEvent event = AuditEvent.builder()
                .auditEventId(auditEventId)
                .decisionType(req.decisionType())
                .systemPrompt(req.systemPrompt())
                .inputHash(req.inputHash())
                .outputHash(req.outputHash())
                .confidenceScore(req.confidenceScore())
                .decisionLatencyMs(req.decisionLatencyMs())
                .currentEntryHash(currentHash)
                .previousEntryHash(previousHash)
                .modelRegistry(model)
                .complianceInfo(compliance)
                .actorInfo(actor)
                .build();

        AuditEvent saved = auditRepo.save(event);
        return CreateAuditEventResponse.builder()
                .id(saved.getId())
                .auditEventId(saved.getAuditEventId())
                .status("SUCCESS")
                .message("Audit event created")
                .build();
    }

    @Transactional(readOnly = true)
    public AuditEventDetailResponse getDetail(String auditEventId) {
        AuditEvent e = findOrThrow(auditEventId);
        ModelRegistry m = e.getModelRegistry();
        ComplianceInfo c = e.getComplianceInfo();
        ActorInfo a = e.getActorInfo();

        return AuditEventDetailResponse.builder()
                // Core Identifiers
                .auditEventId(e.getAuditEventId())
                .timestamp(e.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                .correlationId(e.getCorrelationId())
                .previousEntryHash(e.getPreviousEntryHash())
                .currentEntryHash(e.getCurrentEntryHash())
                .riskLevel(resolveRiskLevel(e.getConfidenceScore(), e.getDecisionType()))
                // User & Interaction Context
                .initiatingUserId(a.getInitiatingUserId())
                .userRole(a.getUserRole())
                .channel(a.getChannel())
                .cbsReferenceId(a.getCbsReferenceId())
                .sessionId(a.getSessionId())
                .clientIpHash(a.getClientIpHash())
                // Model Governance
                .modelId(m.getModelId())
                .modelVersion(m.getModelVersion())
                .provider(m.getProvider())
                .modelType(m.getModelType())
                .systemPrompt(e.getSystemPrompt())
                // Security & Privacy
                .piiCategory(c.getPiiCategory())
                .piiAccessFlag(c.isPiiAccessFlag())
                .consentTokenReference(c.getConsentTokenReference())
                .consentPurposeCode(c.getConsentPurposeCode())
                .externalLlmUsageFlag(c.isExternalLlmUsageFlag())
                .customerAiDisclosureDeliveredFlag(c.isCustomerAiDisclosureDeliveredFlag())
                // Decision Metrics
                .decisionType(e.getDecisionType())
                .confidenceScore(e.getConfidenceScore())
                .decisionLatencyMs(e.getDecisionLatencyMs())
                // Input / Output
                .inputHash(e.getInputHash())
                .outputHash(e.getOutputHash())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditEventResponse getByAuditEventId(String auditEventId) {
        return toResponse(findOrThrow(auditEventId));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(String auditEventId, String decisionType, String modelId,
                                            String userId, String channel,
                                            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditRepo.findAll(
                AuditEventSpec.filter(auditEventId, decisionType, modelId, userId, channel, from, to),
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ChainVerifyResponse verify(String auditEventId) {
        AuditEvent event = findOrThrow(auditEventId);

        String expectedHash = sha256(
                event.getAuditEventId() + event.getInputHash() + event.getOutputHash() +
                event.getDecisionType() + event.getActorInfo().getInitiatingUserId() +
                event.getPreviousEntryHash()
        );

        boolean currentHashVerified = expectedHash.equals(event.getCurrentEntryHash());

        boolean previousHashVerified = auditRepo.findTopByOrderByCreatedAtDesc()
                .filter(prev -> !prev.getAuditEventId().equals(auditEventId))
                .map(prev -> prev.getCurrentEntryHash().equals(event.getPreviousEntryHash()))
                .orElse(event.getPreviousEntryHash().equals(GENESIS_HASH));

        return ChainVerifyResponse.builder()
                .auditEventId(auditEventId)
                .chainValid(currentHashVerified && previousHashVerified)
                .currentHashVerified(currentHashVerified)
                .previousHashVerified(previousHashVerified)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuditEvent findOrThrow(String auditEventId) {
        return auditRepo.findByAuditEventId(auditEventId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditEvent", auditEventId));
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return AuditEventResponse.builder()
                .auditEventId(e.getAuditEventId())
                .decisionType(e.getDecisionType())
                .timestamp(e.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                .date(e.getTimestamp().format(DATE_FMT))
                .modelId(e.getModelRegistry().getModelId())
                .confidenceScore(e.getConfidenceScore())
                .riskLevel(resolveRiskLevel(e.getConfidenceScore(), e.getDecisionType()))
                .correlationId(e.getCorrelationId())
                .inputHash(e.getInputHash())
                .outputHash(e.getOutputHash())
                .decisionLatencyMs(e.getDecisionLatencyMs())
                .currentEntryHash(e.getCurrentEntryHash())
                .previousEntryHash(e.getPreviousEntryHash())
                .build();
    }

    static String resolveRiskLevel(double confidenceScore, String decisionType) {
        if ("ESCALATE".equals(decisionType) || "HIGH_RISK".equals(decisionType)) return "CRITICAL";
        if ("DENY".equals(decisionType))                                           return "HIGH";
        if (confidenceScore < 0.5)                                                 return "CRITICAL";
        if (confidenceScore < 0.7)                                                 return "HIGH";
        if (confidenceScore < 0.85)                                                return "MEDIUM";
        return "LOW";
    }

    private String generateAuditEventId() {
        return "AUD-" + LocalDateTime.now().format(ID_FMT) + "-" +
               String.format("%06d", sequence.incrementAndGet());
    }

    private static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
