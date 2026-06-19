package com.sushil.service;

import com.sushil.config.CacheConfig;
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
import org.springframework.cache.annotation.CacheEvict;
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
        if (userRepository.existsByEmail(req.email()))
            throw new DuplicateResourceException("Email '%s' is already in use".formatted(req.email()));
        if (req.mobile() != null && userRepository.existsByMobile(req.mobile()))
            throw new DuplicateResourceException("Mobile '%s' is already in use".formatted(req.mobile()));

        String username = generateUsername(req.firstName(), req.lastName());
        User saved = userRepository.save(User.builder()
                .username(username).firstName(req.firstName()).lastName(req.lastName())
                .email(req.email()).mobile(req.mobile())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role()).build());

        log.info("[AUTH-SVC] Registered userId={}, username='{}', role={}", saved.getId(), username, saved.getRole());

        if (profilePic != null && !profilePic.isEmpty()) {
            storageService.storeProfilePicAsync(profilePic).thenAccept(url ->
                userRepository.findById(saved.getId()).ifPresent(u -> {
                    u.setProfilePicUrl(url);
                    userRepository.save(u);
                })
            ).exceptionally(ex -> {
                log.error("[AUTH-SVC] Async profile pic upload failed userId={}: {}", saved.getId(), ex.getMessage());
                return null;
            });
        }

        String maskedChannel = otpService.generateAndSend(saved);
        String loginToken    = jwtUtil.generateOtpPendingToken(saved.getUsername());
        return LoginResponse.builder().otpRequired(true)
                .message("Registration successful. OTP sent to %s".formatted(maskedChannel))
                .loginToken(loginToken).build();
    }

    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletRequest httpReq) {
        LoginMode mode = detectMode(req.username(), req.email(), req.mobile());
        User user = resolveUser(req, mode);

        if (mode != LoginMode.MOBILE) authenticatePassword(user, req.password());

        if (!isOtpRequiredToday(user)) {
            log.info("[AUTH-SVC] Session exists today, OTP skipped — userId={}", user.getId());
            return issueTokens(user, loginMethodLabel(mode), resolveIp(httpReq), httpReq.getHeader("User-Agent"));
        }

        String maskedChannel = otpService.generateAndSend(user);
        String loginToken    = jwtUtil.generateOtpPendingToken(user.getUsername());
        return LoginResponse.builder().otpRequired(true)
                .message("OTP sent to %s.".formatted(maskedChannel)).loginToken(loginToken).build();
    }

    @Transactional
    public LoginResponse verifyOtp(OtpVerifyRequest req, HttpServletRequest httpReq) {
        if (!jwtUtil.isOtpPendingToken(req.loginToken()))
            throw new InvalidTokenException("Invalid or expired login token. Please login again.");

        String username = jwtUtil.extractUsername(req.loginToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        otpService.verify(user, req.otp());

        boolean isFirstSession = !sessionRepository.existsByUserId(user.getId());
        String method = isFirstSession ? "REGISTER_OTP_VERIFIED" : "LOGIN_OTP_VERIFIED";
        return issueTokens(user, method, resolveIp(httpReq), httpReq.getHeader("User-Agent"));
    }

    @CacheEvict(value = CacheConfig.TOKEN_ACTIVE, key = "#token")
    @Transactional
    public void logout(String token) {
        sessionRepository.findByToken(token).ifPresentOrElse(
            session -> { session.setRevoked(true); sessionRepository.save(session); },
            () -> log.warn("[AUTH-SVC] Logout — no session for token='{}'", maskToken(token))
        );
    }

    @CacheEvict(value = CacheConfig.TOKEN_ACTIVE, allEntries = true)
    @Transactional
    public void logoutAll(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
            user -> sessionRepository.revokeAllUserSessions(user.getId()),
            () -> log.warn("[AUTH-SVC] logoutAll — user '{}' not found", username)
        );
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private enum LoginMode { USERNAME, EMAIL, MOBILE }

    private LoginMode detectMode(String username, String email, String mobile) {
        int count = (StringUtils.hasText(username) ? 1 : 0)
                  + (StringUtils.hasText(email)    ? 1 : 0)
                  + (StringUtils.hasText(mobile)   ? 1 : 0);
        if (count != 1) throw new BadRequestException("Provide exactly one of: username, email, or mobile.");
        if (StringUtils.hasText(username)) return LoginMode.USERNAME;
        if (StringUtils.hasText(email))    return LoginMode.EMAIL;
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
        if (!StringUtils.hasText(rawPassword))
            throw new BadRequestException("Password is required for username/email login.");
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), rawPassword));
    }

    private LoginResponse issueTokens(User user, String loginMethod, String ip, String userAgent) {
        Set<String> permNames = user.effectivePermissions().stream()
                .map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new));
        String accessToken  = jwtUtil.generateToken(user.getUsername(),
                Map.of("role", user.getRole().name(), "permissions", permNames));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        sessionRepository.save(UserSession.builder()
                .user(user).token(accessToken).loginMethod(loginMethod)
                .loginIp(ip).userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusDays(SESSION_TTL_DAYS)).build());

        return LoginResponse.builder().otpRequired(false).message("Login successful.")
                .accessToken(accessToken).refreshToken(refreshToken).build();
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

    /**
     * Generates unique username with a single COUNT query instead of N+1 existsByUsername loop.
     * Uses functional approach: base stays if count==0, otherwise appends suffix from count.
     */
    private String generateUsername(String firstName, String lastName) {
        String base = firstName.toLowerCase().replaceAll("[^a-z0-9]", "")
                + "." + lastName.toLowerCase().replaceAll("[^a-z0-9]", "");
        long count = userRepository.countByUsernameStartingWith(base);
        return count == 0 ? base : base + (count + 1);
    }

    private static String maskToken(String token) {
        return (token != null && token.length() > 20) ? token.substring(0, 20) + "...[JWT]" : token;
    }
}
