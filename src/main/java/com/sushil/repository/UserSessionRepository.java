package com.sushil.repository;

import com.sushil.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Transactional(readOnly = true)
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByToken(String token);

    @Query("SELECT COUNT(s) > 0 FROM UserSession s WHERE s.token = :token AND s.revoked = false")
    boolean isTokenActive(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId")
    void revokeAllUserSessions(@Param("userId") Long userId);

    /**
     * True if user has ANY session (active or revoked) created on or after the
     * start of today — used to skip OTP on repeat logins within the same day.
     */
    @Query("SELECT COUNT(s) > 0 FROM UserSession s WHERE s.user.id = :userId AND s.createdAt >= :startOfDay")
    boolean hasSessionCreatedToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay);

    boolean existsByUserId(Long userId);
}
