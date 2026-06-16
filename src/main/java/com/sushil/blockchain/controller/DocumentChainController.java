package com.sushil.blockchain.controller;

import com.sushil.blockchain.dto.BlockchainDto.DocVerifyResult;
import com.sushil.blockchain.entity.DocRecord;
import com.sushil.blockchain.service.DocumentChainService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/blockchain/docs")
@RequiredArgsConstructor
public class DocumentChainController {

    private final DocumentChainService documentChainService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocRecord>>> getAllDocs(HttpServletRequest req) {
        List<DocRecord> docs = documentChainService.getAllDocs();
        return ResponseEntity.ok(ApiResponse.success(docs, "OK", HttpStatus.OK, req.getRequestURI()));
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
        DocVerifyResult result = documentChainService.verifyDoc(file);
        return ResponseEntity.ok(ApiResponse.success(result, "OK", HttpStatus.OK, req.getRequestURI()));
    }
}
