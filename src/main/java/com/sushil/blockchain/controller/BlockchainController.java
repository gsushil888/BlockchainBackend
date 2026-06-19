package com.sushil.blockchain.controller;

import com.sushil.blockchain.dto.BlockchainDto.*;
import com.sushil.blockchain.entity.Block;
import com.sushil.blockchain.service.BlockchainService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockchainController {

    private final BlockchainService blockchainService;

    @GetMapping("/chain")
    public ResponseEntity<ApiResponse<Page<Block>>> getChain(
            @PageableDefault(size = 50, sort = "index") Pageable pageable,
            HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                blockchainService.getChain(pageable), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ChainStats>> getStats(HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                blockchainService.stats(), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @GetMapping("/block/{index}")
    public ResponseEntity<ApiResponse<Block>> getBlock(@PathVariable int index, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                blockchainService.getByIndex(index), "OK", HttpStatus.OK, req.getRequestURI()));
    }

    /** Returns immediately with 202 ACCEPTED while mining runs async on taskExecutor. */
    @PostMapping("/mine")
    public CompletableFuture<ResponseEntity<ApiResponse<Block>>> mine(
            @Valid @RequestBody MineBlockRequest body,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest req) {
        log.info("[BLOCKCHAIN] Mine requested by user={}", user.getUsername());
        return blockchainService.mineNext(body, user.getUsername())
                .thenApply(block -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(block, "Block mined successfully", HttpStatus.CREATED, req.getRequestURI())));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyResult>> verify(HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                blockchainService.verify(), "OK", HttpStatus.OK, req.getRequestURI()));
    }
}
