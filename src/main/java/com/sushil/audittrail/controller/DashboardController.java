package com.sushil.audittrail.controller;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.service.DashboardService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    /**
     * GET /api/v1/dashboard/summary?window=TODAY|LAST_7_DAYS|LAST_30_DAYS
     * Returns all 5 KPI counts + decision breakdown + model activity.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @RequestParam(defaultValue = "TODAY") DashboardWindow window,
            HttpServletRequest req) {

        return ResponseEntity.ok(
                ApiResponse.success(service.summary(window), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    /**
     * GET /api/v1/dashboard/events?window=TODAY|LAST_7_DAYS|LAST_30_DAYS&page=0&size=20
     * Paginated audit event table for the dashboard grid.
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<DashboardAuditEventRow>>> events(
            @RequestParam(defaultValue = "TODAY") DashboardWindow window,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest req) {

        return ResponseEntity.ok(
                ApiResponse.success(service.auditEventTable(window, pageable), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    /**
     * GET /api/v1/dashboard/exceptions?window=TODAY|LAST_7_DAYS|LAST_30_DAYS
     * Paginated failed AI responses and system exceptions.
     */
    @GetMapping("/exceptions")
    public ResponseEntity<ApiResponse<Page<ExceptionLogRow>>> exceptions(
            @RequestParam(defaultValue = "TODAY") DashboardWindow window,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest req) {

        return ResponseEntity.ok(
                ApiResponse.success(service.exceptionLog(window, pageable), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    /**
     * GET /api/v1/dashboard/export?window=TODAY|LAST_7_DAYS|LAST_30_DAYS
     * Downloads audit events as CSV file.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "TODAY") DashboardWindow window) {

        byte[] csv = service.exportCsv(window);
        String filename = "audit-events-" + window.name().toLowerCase() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
