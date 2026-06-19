package com.sushil.blockchain.repository;

import com.sushil.blockchain.entity.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByIndex(int index);

    boolean existsByIndex(int index);

    /** Fetch paginated chain ordered by index — avoids loading entire chain in memory. */
    Page<Block> findAllByOrderByIndexAsc(Pageable pageable);

    /** Get only the latest block — used by mineNext without loading whole chain. */
    @Query("SELECT b FROM Block b ORDER BY b.index DESC LIMIT 1")
    Optional<Block> findLatestBlock();

    @Query("SELECT COUNT(b) FROM Block b")
    long countBlocks();
}
