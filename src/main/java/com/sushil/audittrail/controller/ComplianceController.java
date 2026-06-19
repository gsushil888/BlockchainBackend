package com.sushil.audittrail.controller;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.service.ComplianceService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService service;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ComplianceSummaryResponse>> summary(HttpServletRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(service.summary(), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @GetMapping("/pii-report")
    public ResponseEntity<ApiResponse<Map<String, Long>>> piiReport(HttpServletRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(service.piiReport(), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @GetMapping("/consent-report")
    public ResponseEntity<ApiResponse<ConsentReportResponse>> consentReport(HttpServletRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(service.consentReport(), "OK", HttpStatus.OK, req.getRequestURI()));
    }
}
