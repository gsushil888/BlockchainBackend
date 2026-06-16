package com.sushil.service;

import com.sushil.entity.OtpSession;
import com.sushil.entity.User;
import com.sushil.exception.AppExceptions.OtpException;
import com.sushil.repository.OtpSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    @Value("${app.otp.ttl-minutes:5}")
    private int otpTtlMinutes;

    private static final SecureRandom RNG = new SecureRandom();

    private final OtpSessionRepository otpSessionRepository;

    /** Generates and dispatches a fresh OTP. Returns masked channel description for response message. */
    @Transactional
    public String generateAndSend(User user) {
        otpSessionRepository.deleteAllByUserId(user.getId());

        String code = String.format("%06d", RNG.nextInt(1_000_000));
        otpSessionRepository.save(OtpSession.builder()
                .user(user)
                .otpCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(otpTtlMinutes))
                .build());

        String channel;
        String maskedChannel;
        if (user.getMobile() != null) {
            channel = "mobile=" + user.getMobile();
            maskedChannel = maskMobile(user.getMobile());
        } else {
            channel = "email=" + user.getEmail();
            maskedChannel = maskEmail(user.getEmail());
        }

        // TODO: plug in Twilio / AWS SNS / SES
        log.info("[OTP-SVC] OTP dispatched to {} for userId={} (stub — code={})", channel, user.getId(), code);
        return maskedChannel;
    }

    /** Verifies OTP. Throws OtpException on any failure. */
    @Transactional
    public void verify(User user, String inputCode) {
        OtpSession session = otpSessionRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new OtpException("No OTP found. Please initiate login again."));

        if (session.isVerified()) {
            throw new OtpException("OTP already used.");
        }
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new OtpException("OTP has expired. Please initiate login again.");
        }
        if (!session.getOtpCode().equals(inputCode)) {
            throw new OtpException("Invalid OTP.");
        }

        session.setVerified(true);
        otpSessionRepository.save(session);
        log.info("[OTP-SVC] OTP verified for userId={}", user.getId());
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private static String maskMobile(String mobile) {
        if (mobile.length() <= 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at);
    }
}
