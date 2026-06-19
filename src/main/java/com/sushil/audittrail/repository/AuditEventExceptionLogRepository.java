package com.sushil.audittrail.repository;

import com.sushil.audittrail.entity.AuditEventExceptionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditEventExceptionLogRepository extends JpaRepository<AuditEventExceptionLog, String> {

    long countByExceptionTypeAndCreatedAtAfter(String exceptionType, LocalDateTime after);

    @Query("SELECT e FROM AuditEventExceptionLog e WHERE e.createdAt >= :after ORDER BY e.createdAt DESC")
    Page<AuditEventExceptionLog> findRecentExceptions(@Param("after") LocalDateTime after, Pageable pageable);
}
