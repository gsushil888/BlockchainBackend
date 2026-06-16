package com.sushil.blockchain.service;

import com.sushil.blockchain.dto.BlockchainDto.DocVerifyResult;
import com.sushil.blockchain.entity.Block;
import com.sushil.blockchain.entity.DocRecord;
import com.sushil.blockchain.repository.DocRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChainService {

    private final DocRecordRepository docRecordRepository;
    private final BlockchainService blockchainService;

    @Transactional(readOnly = true)
    public List<DocRecord> getAllDocs() {
        return docRecordRepository.findAll();
    }

    @Transactional
    public DocRecord uploadDoc(MultipartFile file, String uploader) throws IOException, NoSuchAlgorithmException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided or empty file.");
        }

        String sha256 = computeSha256(file.getBytes());
        String data = "DOC:" + file.getOriginalFilename() + ":" + sha256;

        Block block = blockchainService.mineDocBlock(data, uploader);

        DocRecord record = DocRecord.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .sha256Hash(sha256)
                .blockIndex(block.getIndex())
                .blockHash(block.getHash())
                .uploadedBy(uploader)
                .build();

        DocRecord saved = docRecordRepository.save(record);
        log.info("[DOC-CHAIN] Stored doc='{}' sha256={} blockIndex={}", saved.getFileName(), sha256, block.getIndex());
        return saved;
    }

    @Transactional(readOnly = true)
    public DocVerifyResult verifyDoc(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided or empty file.");
        }

        String sha256 = computeSha256(file.getBytes());
        Optional<DocRecord> match = docRecordRepository.findBySha256Hash(sha256);

        if (match.isPresent()) {
            DocRecord rec = match.get();
            return DocVerifyResult.builder()
                    .verified(true)
                    .fileName(file.getOriginalFilename())
                    .sha256Hash(sha256)
                    .blockIndex(rec.getBlockIndex())
                    .blockHash(rec.getBlockHash())
                    .timestamp(rec.getTimestamp())
                    .message("Document hash found on block #" + rec.getBlockIndex() + ".")
                    .build();
        }

        return DocVerifyResult.builder()
                .verified(false)
                .fileName(file.getOriginalFilename())
                .sha256Hash(sha256)
                .message("No matching document hash found on the blockchain.")
                .build();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static String computeSha256(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
