package com.sushil.blockchain.repository;

import com.sushil.blockchain.entity.DocRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocRecordRepository extends JpaRepository<DocRecord, String> {
    Optional<DocRecord> findBySha256Hash(String sha256Hash);
}
