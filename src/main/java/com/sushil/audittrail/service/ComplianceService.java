package com.sushil.audittrail.service;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final AuditEventRepository repo;

    @Transactional(readOnly = true)
    public ComplianceSummaryResponse summary() {
        long total = repo.count();
        return ComplianceSummaryResponse.builder()
                .totalAuditRecords(total)
                .piiEvents(repo.countByComplianceInfoPiiAccessFlagTrue())
                .missingConsent(repo.countByComplianceInfoConsentTokenReferenceIsNull())
                .externalLlmUsage(repo.countByComplianceInfoExternalLlmUsageFlagTrue())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> piiReport() {
        return repo.countByPiiCategory().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public ConsentReportResponse consentReport() {
        long total = repo.count();
        long missing = repo.countByComplianceInfoConsentTokenReferenceIsNull();
        return ConsentReportResponse.builder()
                .total(total)
                .validConsent(total - missing)
                .missingConsent(missing)
                .build();
    }
}
