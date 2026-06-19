package com.sushil.audittrail.controller;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.service.AuditEventService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateAuditEventResponse>> create(
            @Valid @RequestBody CreateAuditEventRequest req, HttpServletRequest httpReq) {

        CreateAuditEventResponse body = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(body, "Audit event created", HttpStatus.CREATED, httpReq.getRequestURI()));
    }

    @GetMapping("/{auditEventId}")
    public ResponseEntity<ApiResponse<AuditEventDetailResponse>> get(
            @PathVariable String auditEventId, HttpServletRequest httpReq) {

        return ResponseEntity.ok(
                ApiResponse.success(service.getDetail(auditEventId), "OK", HttpStatus.OK, httpReq.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditEventResponse>>> search(
            @RequestParam(required = false) String auditEventId,
            @RequestParam(required = false) String decisionType,
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpReq) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        service.search(auditEventId, decisionType, modelId, userId, channel, fromDate, toDate, pageable),
                        "OK", HttpStatus.OK, httpReq.getRequestURI()));
    }

    @PostMapping("/{auditEventId}/verify")
    public ResponseEntity<ApiResponse<ChainVerifyResponse>> verify(
            @PathVariable String auditEventId, HttpServletRequest httpReq) {

        return ResponseEntity.ok(
                ApiResponse.success(service.verify(auditEventId), "Chain verified", HttpStatus.OK, httpReq.getRequestURI()));
    }
}
