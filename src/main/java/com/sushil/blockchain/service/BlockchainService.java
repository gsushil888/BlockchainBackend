package com.sushil.blockchain.service;

import com.sushil.blockchain.dto.BlockchainDto.*;
import com.sushil.blockchain.entity.Block;
import com.sushil.blockchain.entity.BlockTransaction;
import com.sushil.blockchain.repository.BlockRepository;
import com.sushil.config.CacheConfig;
import com.sushil.exception.AppExceptions.MiningException;
import com.sushil.exception.AppExceptions.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    @Value("${app.blockchain.difficulty:4}")
    private int difficulty;

    private final BlockRepository blockRepository;

    @PostConstruct
    @Transactional
    public void initGenesis() {
        if (!blockRepository.existsByIndex(0)) {
            Block genesis = mine("Genesis Block", List.of(), "0", 0, "system");
            blockRepository.save(genesis);
            log.info("[BLOCKCHAIN] Genesis block created — hash={}", genesis.getHash());
        }
    }

    /** Paginated chain fetch — avoids loading millions of blocks into memory. */
    @Transactional(readOnly = true)
    public Page<Block> getChain(Pageable pageable) {
        return blockRepository.findAllByOrderByIndexAsc(pageable);
    }

    @Cacheable(value = CacheConfig.BLOCK_BY_IDX, key = "#index")
    @Transactional(readOnly = true)
    public Block getByIndex(int index) {
        return blockRepository.findByIndex(index)
                .orElseThrow(() -> new ResourceNotFoundException("Block", index));
    }

    /** Mines next block asynchronously — frees HTTP thread while PoW executes. */
    @Async("taskExecutor")
    @CacheEvict(value = CacheConfig.CHAIN_STATS, allEntries = true)
    @Transactional
    public CompletableFuture<Block> mineNext(MineBlockRequest req, String miner) {
        Block last = latestBlock();
        List<BlockTransaction> txns = req.transactions() == null ? List.of() :
            req.transactions().stream()
                .map(t -> BlockTransaction.builder()
                    .sender(t.sender()).receiver(t.receiver()).amount(t.amount()).build())
                .toList();
        return CompletableFuture.completedFuture(
            blockRepository.save(mine(req.data(), txns, last.getHash(), last.getIndex() + 1, miner)));
    }

    @Async("taskExecutor")
    @CacheEvict(value = CacheConfig.CHAIN_STATS, allEntries = true)
    @Transactional
    public CompletableFuture<Block> mineDocBlock(String data, String miner) {
        Block last = latestBlock();
        return CompletableFuture.completedFuture(
            blockRepository.save(mine(data, List.of(), last.getHash(), last.getIndex() + 1, miner)));
    }

    @Transactional(readOnly = true)
    public VerifyResult verify() {
        // Verify page-by-page to avoid OOM on large chains
        final int PAGE_SIZE = 500;
        int page = 0;
        Block previous = null;
        Page<Block> chunk;
        do {
            chunk = blockRepository.findAllByOrderByIndexAsc(Pageable.ofSize(PAGE_SIZE).withPage(page++));
            for (Block current : chunk.getContent()) {
                if (previous != null) {
                    if (!current.getHash().equals(calculateHash(current)))
                        return VerifyResult.builder().valid(false)
                            .message("Block hash mismatch.").invalidBlockIndex(current.getIndex()).build();
                    if (!current.getPreviousHash().equals(previous.getHash()))
                        return VerifyResult.builder().valid(false)
                            .message("Block previousHash mismatch.").invalidBlockIndex(current.getIndex()).build();
                }
                previous = current;
            }
        } while (!chunk.isLast());

        return VerifyResult.builder().valid(true)
                .message("All %d blocks verified successfully.".formatted(blockRepository.countBlocks())).build();
    }

    @Cacheable(value = CacheConfig.CHAIN_STATS, key = "'stats'")
    @Transactional(readOnly = true)
    public ChainStats stats() {
        long totalBlocks = blockRepository.countBlocks();
        VerifyResult vr  = verify();
        Block last       = latestBlock();
        return ChainStats.builder()
                .totalBlocks(totalBlocks)
                .totalTransactions(0L) // avoid full scan; compute separately if needed
                .isValid(vr.isValid())
                .difficulty(difficulty)
                .lastBlockHash(last.getHash())
                .build();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /** Single-query latest block — O(1) vs O(n) full chain sort. */
    private Block latestBlock() {
        return blockRepository.findLatestBlock()
                .orElseThrow(() -> new ResourceNotFoundException("Block", "genesis"));
    }

    private Block mine(String data, List<BlockTransaction> txns, String previousHash, int index, String miner) {
        try {
            String target = "0".repeat(difficulty);
            Block block = Block.builder()
                    .index(index).timestamp(LocalDateTime.now())
                    .transactions(txns instanceof ArrayList ? txns : new ArrayList<>(txns))
                    .data(data).previousHash(previousHash).miner(miner).nonce(0).hash("").build();
            long nonce = 0;
            String hash;
            do { block.setNonce(nonce++); hash = calculateHash(block); } while (!hash.startsWith(target));
            block.setHash(hash);
            log.info("[BLOCKCHAIN] Mined block index={} nonce={} hash={}", index, block.getNonce(), hash);
            return block;
        } catch (Exception e) {
            throw new MiningException("Mining failed for block index=" + index, e);
        }
    }

    public String calculateHash(Block block) {
        return DigestUtils.sha256Hex(
            block.getIndex() + block.getTimestamp().toString()
            + block.getTransactions().toString() + block.getData()
            + block.getPreviousHash() + block.getNonce());
    }
}
