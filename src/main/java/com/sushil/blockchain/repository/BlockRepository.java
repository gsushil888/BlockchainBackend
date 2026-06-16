package com.sushil.blockchain.repository;

import com.sushil.blockchain.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Optional<Block> findByIndex(int index);
    boolean existsByIndex(int index);
}
