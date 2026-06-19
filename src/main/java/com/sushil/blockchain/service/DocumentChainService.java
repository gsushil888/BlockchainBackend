package com.sushil.blockchain.service;

import com.sushil.blockchain.dto.BlockchainDto.DocVerifyResult;
import com.sushil.blockchain.entity.Block;
import com.sushil.blockchain.entity.DocRecord;
import com.sushil.blockchain.repository.DocRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChainService {

    private final DocRecordRepository docRecordRepository;
    private final BlockchainService   blockchainService;

    @Transactional(readOnly = true)
    public Page<DocRecord> getAllDocs(Pageable pageable) {
        return docRecordRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional
    public DocRecord uploadDoc(MultipartFile file, String uploader)
            throws IOException, NoSuchAlgorithmException, ExecutionException, InterruptedException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file provided or empty file.");

        String sha256 = computeSha256(file.getBytes());
        String data   = "DOC:" + file.getOriginalFilename() + ":" + sha256;

        // Block on async mining — transaction waits for block to be persisted
        Block block = blockchainService.mineDocBlock(data, uploader).get();

        DocRecord saved = docRecordRepository.save(DocRecord.builder()
                .fileName(file.getOriginalFilename()).fileType(file.getContentType())
                .fileSize(file.getSize()).sha256Hash(sha256)
                .blockIndex(block.getIndex()).blockHash(block.getHash())
                .uploadedBy(uploader).build());

        log.info("[DOC-CHAIN] Stored doc='{}' sha256={} blockIndex={}", saved.getFileName(), sha256, block.getIndex());
        return saved;
    }

    @Transactional(readOnly = true)
    public DocVerifyResult verifyDoc(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file provided or empty file.");

        String sha256 = computeSha256(file.getBytes());
        Optional<DocRecord> match = docRecordRepository.findBySha256Hash(sha256);

        return match.map(rec -> DocVerifyResult.builder()
                .verified(true).fileName(file.getOriginalFilename()).sha256Hash(sha256)
                .blockIndex(rec.getBlockIndex()).blockHash(rec.getBlockHash())
                .timestamp(rec.getTimestamp())
                .message("Document hash found on block #" + rec.getBlockIndex() + ".")
                .build())
            .orElseGet(() -> DocVerifyResult.builder()
                .verified(false).fileName(file.getOriginalFilename()).sha256Hash(sha256)
                .message("No matching document hash found on the blockchain.")
                .build());
    }

    private static String computeSha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
