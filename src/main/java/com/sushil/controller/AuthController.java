package com.sushil.controller;

import com.sushil.dto.ApiResponse;
import com.sushil.dto.AuthDto.*;
import com.sushil.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @ModelAttribute RegisterRequest req,
            @RequestPart(value = "profilePic", required = false) MultipartFile profilePic,
            HttpServletRequest request) {

        log.info("[AUTH] Register — email='{}', name='{} {}'", req.email(), req.firstName(), req.lastName());
        LoginResponse body = authService.register(req, profilePic);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(body, body.getMessage(), HttpStatus.CREATED, request.getRequestURI()));
    }

    /**
     * Login — credentials only, no OTP here.
     *
     * Provide exactly one of: username, email, mobile.
     *   { "username": "john.smith", "password": "secret" }
     *   { "email": "john@example.com", "password": "secret" }
     *   { "mobile": "+911234567890" }
     *
     * Response A — session exists today (OTP skipped):
     *   { "otpRequired": false, "accessToken": "...", "refreshToken": "...", ... }
     *
     * Response B — first login today (OTP required):
     *   { "otpRequired": true, "loginToken": "...", "message": "OTP sent..." }
     *   → Call POST /verify-otp with loginToken + otp to get tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest request) {

        log.info("[AUTH] Login — username='{}', email='{}', mobile='{}'",
                req.username(), req.email(), req.mobile());
        LoginResponse body = authService.login(req, request);
        return ResponseEntity.ok(ApiResponse.success(body, body.getMessage(), HttpStatus.OK, request.getRequestURI()));
    }

    /**
     * OTP verification — called only when login returned otpRequired=true.
     *
     *   { "loginToken": "<token from login response>", "otp": "482910" }
     *
     * Response: full tokens on success.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest req, HttpServletRequest request) {

        log.info("[AUTH] OTP verify — loginToken present={}", req.loginToken() != null);
        LoginResponse body = authService.verifyOtp(req, request);
        return ResponseEntity.ok(ApiResponse.success(body, body.getMessage(), HttpStatus.OK, request.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader, HttpServletRequest request) {

        String token = extractToken(authHeader);
        authService.logout(token);
        log.info("[AUTH] Logout — token revoked");
        return ResponseEntity.ok(
                ApiResponse.success(null, "Logged out successfully", HttpStatus.OK, request.getRequestURI()));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestParam String username, HttpServletRequest request) {

        authService.logoutAll(username);
        log.info("[AUTH] All sessions revoked for username='{}'", username);
        return ResponseEntity.ok(
                ApiResponse.success(null, "All sessions revoked", HttpStatus.OK, request.getRequestURI()));
    }

    private static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }
}
