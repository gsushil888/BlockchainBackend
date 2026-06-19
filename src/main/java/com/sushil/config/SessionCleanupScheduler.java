package com.sushil.config;

import com.sushil.repository.OtpSessionRepository;
import com.sushil.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    private final OtpSessionRepository  otpSessionRepository;
    private final UserSessionRepository userSessionRepository;

    /** Runs every 30 minutes — removes expired OTPs and revoked/expired sessions. */
    @Scheduled(fixedRateString = "PT30M")
    @Transactional
    @CacheEvict(value = CacheConfig.TOKEN_ACTIVE, allEntries = true)
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        otpSessionRepository.deleteExpired(now);
        int deleted = userSessionRepository.deleteExpiredSessions(now);
        log.info("[CLEANUP] Purged {} expired session(s) and stale OTPs", deleted);
    }
}
