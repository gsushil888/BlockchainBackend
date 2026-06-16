package com.sushil.blockchain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doc_records")
public class DocRecord {

    @Id
    private String id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 64)
    private String sha256Hash;

    @Column(nullable = false)
    private int blockIndex;

    @Column(nullable = false, length = 64)
    private String blockHash;

    @Column(nullable = false, length = 50)
    private String uploadedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
