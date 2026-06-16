package com.sushil.blockchain.service;

import com.sushil.blockchain.dto.BlockchainDto.*;
import com.sushil.blockchain.entity.Block;
import com.sushil.blockchain.entity.BlockTransaction;
import com.sushil.blockchain.repository.BlockRepository;
import com.sushil.exception.AppExceptions.MiningException;
import com.sushil.exception.AppExceptions.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Transactional(readOnly = true)
    public List<Block> getChain() {
        return blockRepository.findAll()
                .stream()
                .sorted(java.util.Comparator.comparingInt(Block::getIndex))
                .toList();
    }

    @Transactional(readOnly = true)
    public Block getByIndex(int index) {
        return blockRepository.findByIndex(index)
                .orElseThrow(() -> new ResourceNotFoundException("Block", index));
    }

    @Transactional
    public Block mineNext(MineBlockRequest req, String miner) {
        List<Block> chain = getChain();
        Block last = chain.get(chain.size() - 1);

        List<BlockTransaction> txns = req.transactions() == null ? List.of() :
                req.transactions().stream()
                        .map(t -> BlockTransaction.builder()
                                .sender(t.sender())
                                .receiver(t.receiver())
                                .amount(t.amount())
                                .build())
                        .toList();

        Block block = mine(req.data(), txns, last.getHash(), last.getIndex() + 1, miner);
        return blockRepository.save(block);
    }

    @Transactional
    public Block mineDocBlock(String data, String miner) {
        List<Block> chain = getChain();
        Block last = chain.get(chain.size() - 1);
        Block block = mine(data, List.of(), last.getHash(), last.getIndex() + 1, miner);
        return blockRepository.save(block);
    }

    @Transactional(readOnly = true)
    public VerifyResult verify() {
        List<Block> chain = getChain();
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHash().equals(calculateHash(current))) {
                return VerifyResult.builder()
                        .valid(false)
                        .message("Block hash mismatch detected.")
                        .invalidBlockIndex(current.getIndex())
                        .build();
            }
            if (!current.getPreviousHash().equals(previous.getHash())) {
                return VerifyResult.builder()
                        .valid(false)
                        .message("Block previousHash mismatch detected.")
                        .invalidBlockIndex(current.getIndex())
                        .build();
            }
        }
        return VerifyResult.builder()
                .valid(true)
                .message("All %d blocks verified successfully.".formatted(chain.size()))
                .build();
    }

    @Transactional(readOnly = true)
    public ChainStats stats() {
        List<Block> chain = getChain();
        long totalTx = chain.stream().mapToLong(b -> b.getTransactions().size()).sum();
        VerifyResult vr = verify();
        Block last = chain.get(chain.size() - 1);
        return ChainStats.builder()
                .totalBlocks(chain.size())
                .totalTransactions(totalTx)
                .isValid(vr.isValid())
                .difficulty(difficulty)
                .lastBlockHash(last.getHash())
                .build();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private Block mine(String data, List<BlockTransaction> txns, String previousHash, int index, String miner) {
        try {
            String target = "0".repeat(difficulty);
        Block block = Block.builder()
                .index(index)
                .timestamp(LocalDateTime.now())
                .transactions(txns instanceof java.util.ArrayList ? txns : new java.util.ArrayList<>(txns))
                .data(data)
                .previousHash(previousHash)
                .miner(miner)
                .nonce(0)
                .hash("")
                .build();

        long nonce = 0;
        String hash;
        do {
            block.setNonce(nonce++);
            hash = calculateHash(block);
        } while (!hash.startsWith(target));

            block.setHash(hash);
            log.info("[BLOCKCHAIN] Mined block index={} nonce={} hash={}", index, block.getNonce(), hash);
            return block;
        } catch (Exception e) {
            throw new MiningException("Mining failed for block index=" + index, e);
        }
    }

    public String calculateHash(Block block) {
        String raw = block.getIndex()
                + block.getTimestamp().toString()
                + block.getTransactions().toString()
                + block.getData()
                + block.getPreviousHash()
                + block.getNonce();
        return DigestUtils.sha256Hex(raw);
    }
}
