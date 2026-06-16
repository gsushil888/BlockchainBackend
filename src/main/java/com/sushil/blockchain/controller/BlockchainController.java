package com.sushil.blockchain.controller;

import com.sushil.blockchain.dto.BlockchainDto.*;
import com.sushil.blockchain.entity.Block;
import com.sushil.blockchain.service.BlockchainService;
import com.sushil.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockchainController {

    private final BlockchainService blockchainService;

    @GetMapping("/chain")
    public ResponseEntity<ApiResponse<List<Block>>> getChain(HttpServletRequest req) {
        List<Block> chain = blockchainService.getChain();
        return ResponseEntity.ok(ApiResponse.success(chain, "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ChainStats>> getStats(HttpServletRequest req) {
        ChainStats stats = blockchainService.stats();
        return ResponseEntity.ok(ApiResponse.success(stats, "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @GetMapping("/block/{index}")
    public ResponseEntity<ApiResponse<Block>> getBlock(@PathVariable int index, HttpServletRequest req) {
        Block block = blockchainService.getByIndex(index);
        return ResponseEntity.ok(ApiResponse.success(block, "OK", HttpStatus.OK, req.getRequestURI()));
    }

    @PostMapping("/mine")
    public ResponseEntity<ApiResponse<Block>> mine(
            @Valid @RequestBody MineBlockRequest body,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest req) {

        log.info("[BLOCKCHAIN] Mine requested by user={}", user.getUsername());
        Block block = blockchainService.mineNext(body, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(block, "Block mined successfully", HttpStatus.CREATED, req.getRequestURI()));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyResult>> verify(HttpServletRequest req) {
        VerifyResult result = blockchainService.verify();
        return ResponseEntity.ok(ApiResponse.success(result, "OK", HttpStatus.OK, req.getRequestURI()));
    }
}
