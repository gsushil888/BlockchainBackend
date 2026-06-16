package com.sushil.repository;

import com.sushil.entity.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Transactional(readOnly = true)
public interface OtpSessionRepository extends JpaRepository<OtpSession, Long> {

    Optional<OtpSession> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpSession o WHERE o.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpSession o WHERE o.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
