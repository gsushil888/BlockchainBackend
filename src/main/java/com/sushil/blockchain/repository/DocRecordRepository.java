package com.sushil.blockchain.repository;

import com.sushil.blockchain.entity.DocRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface DocRecordRepository extends JpaRepository<DocRecord, String> {

    Optional<DocRecord> findBySha256Hash(String sha256Hash);

    Page<DocRecord> findAllByOrderByTimestampDesc(Pageable pageable);
}
