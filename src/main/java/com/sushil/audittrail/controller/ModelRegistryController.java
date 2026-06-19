package com.sushil.audittrail.controller;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.entity.ModelRegistry.ModelStatus;
import com.sushil.audittrail.service.ModelRegistryService;
import com.sushil.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelRegistryController {

    private final ModelRegistryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateModelResponse>> create(
            @Valid @RequestBody CreateModelRequest req,
            HttpServletRequest httpReq) {

        CreateModelResponse body = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(body, "Model registered", HttpStatus.CREATED, httpReq.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModelResponse>> getById(
            @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest httpReq) {

        return ResponseEntity.ok(
                ApiResponse.success(service.getById(id), "OK", HttpStatus.OK, httpReq.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ModelResponse>>> search(
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String modelType,
            @PageableDefault(size = 20) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest httpReq) {

        return ResponseEntity.ok(
                ApiResponse.success(service.search(modelId, provider, modelType, pageable),
                        "OK", HttpStatus.OK, httpReq.getRequestURI()));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest httpReq) {

        service.setStatus(id, ModelStatus.ACTIVE);
        return ResponseEntity.ok(ApiResponse.success(null, "Model activated", HttpStatus.OK, httpReq.getRequestURI()));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest httpReq) {

        service.setStatus(id, ModelStatus.INACTIVE);
        return ResponseEntity.ok(ApiResponse.success(null, "Model deactivated", HttpStatus.OK, httpReq.getRequestURI()));
    }
}
