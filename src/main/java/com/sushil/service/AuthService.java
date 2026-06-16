package com.sushil.service;

import com.sushil.dto.AuthDto.*;
import com.sushil.entity.User;
import com.sushil.entity.UserSession;
import com.sushil.exception.AppExceptions.*;
import com.sushil.repository.UserRepository;
import com.sushil.repository.UserSessionRepository;
import com.sushil.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int SESSION_TTL_DAYS = 1;

    private final UserRepository        userRepository;
    private final UserSessionRepository sessionRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final OtpService            otpService;
    private final StorageService        storageService;

    @Transactional
    public LoginResponse register(RegisterRequest req, MultipartFile profilePic) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("Email '%s' is already in use".formatted(req.email()));
        }
        if (req.mobile() != null && userRepository.existsByMobile(req.mobile())) {
            throw new DuplicateResourceException("Mobile '%s' is already in use".formatted(req.mobile()));
        }

        String username = generateUsername(req.firstName(), req.lastName());

        User saved = userRepository.save(User.builder()
                .username(username)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .mobile(req.mobile())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role())
                .build());

        log.info("[AUTH-SVC] Registered user id={}, username='{}', role={}", saved.getId(), username, saved.getRole());

        if (profilePic != null && !profilePic.isEmpty()) {
            storageService.storeProfilePicAsync(profilePic).thenAccept(url -> {
                userRepository.findById(saved.getId()).ifPresent(u -> {
                    u.setProfilePicUrl(url);
                    userRepository.save(u);
                    log.info("[AUTH-SVC] Profile pic updated async for userId={}", u.getId());
                });
            }).exceptionally(ex -> {
                log.error("[AUTH-SVC] Async profile pic upload failed for userId={}: {}", saved.getId(), ex.getMessage());
                return null;
            });
        }

        // New user always requires OTP verification before first access
        String maskedChannel = otpService.generateAndSend(saved);
        String loginToken = jwtUtil.generateOtpPendingToken(saved.getUsername());
        log.info("[AUTH-SVC] Registration OTP dispatched for userId={}", saved.getId());

        return LoginResponse.builder()
                .otpRequired(true)
                .message("Registration successful. OTP sent to %s".formatted(maskedChannel))
                .loginToken(loginToken)
                .build();
    }

    /**
     * Step 1 — credential validation only.
     *
     * Detects login mode from which field is non-blank:
     *   username + password  → username login
     *   email    + password  → email login
     *   mobile               → mobile OTP login (no password)
     *
     * If user has an existing session today → OTP skipped, tokens issued immediately.
     * If no session today                  → OTP dispatched, loginToken returned.
     */
    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletRequest httpReq) {
        LoginMode mode = detectMode(req.username(), req.email(), req.mobile());
        User user = resolveUser(req, mode);

        if (mode != LoginMode.MOBILE) {
            authenticatePassword(user, req.password());
        }

        if (!isOtpRequiredToday(user)) {
            log.info("[AUTH-SVC] Session exists today, OTP skipped — userId={}, mode={}", user.getId(), mode);
            return issueTokens(user, loginMethodLabel(mode), resolveIp(httpReq), httpReq.getHeader("User-Agent"));
        }

        String maskedChannel = otpService.generateAndSend(user);
        String loginToken = jwtUtil.generateOtpPendingToken(user.getUsername());
        log.info("[AUTH-SVC] OTP dispatched, loginToken issued — userId={}, mode={}", user.getId(), mode);

        return LoginResponse.builder()
                .otpRequired(true)
                .message("OTP sent to %s.".formatted(maskedChannel))
                .loginToken(loginToken)
                .build();
    }

    /**
     * Step 2 — OTP verification (separate endpoint).
     *
     * Validates the loginToken from step 1, verifies the OTP, issues real session tokens.
     */
    @Transactional
    public LoginResponse verifyOtp(OtpVerifyRequest req, HttpServletRequest httpReq) {
        if (!jwtUtil.isOtpPendingToken(req.loginToken())) {
            throw new InvalidTokenException("Invalid or expired login token. Please login again.");
        }

        String username = jwtUtil.extractUsername(req.loginToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        otpService.verify(user, req.otp());

        // First ever session = registration flow, otherwise login OTP
        boolean isFirstSession = !sessionRepository.existsByUserId(user.getId());
        String method = isFirstSession ? "REGISTER_OTP_VERIFIED" : "LOGIN_OTP_VERIFIED";

        log.info("[AUTH-SVC] OTP verified, issuing session — userId={}, method='{}'", user.getId(), method);
        return issueTokens(user, method, resolveIp(httpReq), httpReq.getHeader("User-Agent"));
    }

    @Transactional
    public void logout(String token) {
        sessionRepository.findByToken(token).ifPresentOrElse(
                session -> {
                    session.setRevoked(true);
                    sessionRepository.save(session);
                    log.info("[AUTH-SVC] Session revoked — id={}, user='{}'", session.getId(), session.getUser().getUsername());
                },
                () -> log.warn("[AUTH-SVC] Logout — no active session for token='{}'", maskToken(token))
        );
    }

    @Transactional
    public void logoutAll(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    sessionRepository.revokeAllUserSessions(user.getId());
                    log.info("[AUTH-SVC] All sessions revoked for username='{}'", username);
                },
                () -> log.warn("[AUTH-SVC] logoutAll — user '{}' not found", username)
        );
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private enum LoginMode { USERNAME, EMAIL, MOBILE }

    private LoginMode detectMode(String username, String email, String mobile) {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasEmail    = StringUtils.hasText(email);
        boolean hasMobile   = StringUtils.hasText(mobile);

        int count = (hasUsername ? 1 : 0) + (hasEmail ? 1 : 0) + (hasMobile ? 1 : 0);
        if (count != 1) throw new BadRequestException("Provide exactly one of: username, email, or mobile.");

        if (hasUsername) return LoginMode.USERNAME;
        if (hasEmail)    return LoginMode.EMAIL;
        return LoginMode.MOBILE;
    }

    private User resolveUser(LoginRequest req, LoginMode mode) {
        return switch (mode) {
            case USERNAME -> userRepository.findByUsername(req.username())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.username()));
            case EMAIL    -> userRepository.findByEmail(req.email())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.email()));
            case MOBILE   -> userRepository.findByMobile(req.mobile())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.mobile()));
        };
    }

    private boolean isOtpRequiredToday(User user) {
        return !sessionRepository.hasSessionCreatedToday(user.getId(), LocalDate.now().atStartOfDay());
    }

    private void authenticatePassword(User user, String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new BadRequestException("Password is required for username/email login.");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), rawPassword));
    }

    private LoginResponse issueTokens(User user, String loginMethod, String ip, String userAgent) {
        Set<String> permNames = user.effectivePermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String accessToken  = jwtUtil.generateToken(user.getUsername(),
                Map.of("role", user.getRole().name(), "permissions", permNames));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        UserSession session = sessionRepository.save(UserSession.builder()
                .user(user)
                .token(accessToken)
                .loginMethod(loginMethod)
                .loginIp(ip)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusDays(SESSION_TTL_DAYS))
                .build());

        log.info("[AUTH-SVC] Session created — id={}, username='{}', method='{}', ip='{}', expiresAt='{}'",
                session.getId(), user.getUsername(), loginMethod, ip, session.getExpiresAt());

        return LoginResponse.builder()
                .otpRequired(false)
                .message("Login successful.")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private static String loginMethodLabel(LoginMode mode) {
        return switch (mode) {
            case USERNAME -> "LOGIN_PASSWORD_USERNAME";
            case EMAIL    -> "LOGIN_PASSWORD_EMAIL";
            case MOBILE   -> "LOGIN_MOBILE_OTP";
        };
    }

    private static String resolveIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return StringUtils.hasText(forwarded) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }

    private String generateUsername(String firstName, String lastName) {
        String base = firstName.toLowerCase().replaceAll("[^a-z0-9]", "")
                + "." + lastName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (!userRepository.existsByUsername(base)) return base;
        int suffix = 2;
        String candidate;
        do { candidate = base + suffix++; } while (userRepository.existsByUsername(candidate));
        return candidate;
    }

    private static String maskToken(String token) {
        return (token != null && token.length() > 20) ? token.substring(0, 20) + "...[JWT]" : token;
    }
}
