package com.sushil.blockchain.controller;

import com.sushil.blockchain.dto.BlockchainDto.DocVerifyResult;
import com.sushil.blockchain.entity.DocRecord;
import com.sushil.blockchain.service.DocumentChainService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/blockchain/docs")
@RequiredArgsConstructor
public class DocumentChainController {

    private final DocumentChainService documentChainService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocRecord>>> getAllDocs(
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                documentChainService.getAllDocs(pageable), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocRecord>> upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest req) throws Exception {
        log.info("[DOC-CHAIN] Upload requested by user={} file={}", user.getUsername(), file.getOriginalFilename());
        DocRecord record = documentChainService.uploadDoc(file, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(record, "Document stored on blockchain", HttpStatus.CREATED, req.getRequestURI()));
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocVerifyResult>> verify(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest req) throws Exception {
        log.info("[DOC-CHAIN] Verify requested for file={}", file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.success(
                documentChainService.verifyDoc(file), "OK", HttpStatus.OK, req.getRequestURI()));
    }
}
