package com.sushil.blockchain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

public final class BlockchainDto {

    private BlockchainDto() {}

    public record TransactionRequest(
            @NotBlank String sender,
            @NotBlank String receiver,
            @Min(0) double amount
    ) {}

    public record MineBlockRequest(
            @NotBlank(message = "Block data is required.") String data,
            @Valid List<TransactionRequest> transactions
    ) {}

    @Value
    @Builder
    public static class VerifyResult {
        boolean valid;
        String message;
        Integer invalidBlockIndex;
    }

    @Value
    @Builder
    public static class ChainStats {
        long totalBlocks;
        long totalTransactions;
        boolean isValid;
        int difficulty;
        String lastBlockHash;
    }

    @Value
    @Builder
    public static class DocVerifyResult {
        boolean verified;
        String fileName;
        String sha256Hash;
        Integer blockIndex;
        String blockHash;
        LocalDateTime timestamp;
        String message;
    }
}
