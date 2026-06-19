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

    /** Uses index on token column — result cached in JwtFilter via CacheConfig.TOKEN_ACTIVE */
    @Query("SELECT COUNT(s) > 0 FROM UserSession s WHERE s.token = :token AND s.revoked = false AND s.expiresAt > :now")
    boolean isTokenActive(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId AND s.revoked = false")
    void revokeAllUserSessions(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) > 0 FROM UserSession s WHERE s.user.id = :userId AND s.createdAt >= :startOfDay")
    boolean hasSessionCreatedToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay);

    boolean existsByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now AND s.revoked = true")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
}
