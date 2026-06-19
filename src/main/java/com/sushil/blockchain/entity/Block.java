package com.sushil.blockchain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blocks", indexes = {
    @Index(name = "idx_block_index", columnList = "index")
})
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int index;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "block_id")
    @Builder.Default
    private List<BlockTransaction> transactions = new ArrayList<>();

    @Column(nullable = false, length = 1000)
    private String data;

    @Column(nullable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false)
    private long nonce;

    @Column(nullable = false, length = 50)
    private String miner;
}
