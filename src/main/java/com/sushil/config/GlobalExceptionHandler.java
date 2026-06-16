package com.sushil.config;

import com.sushil.dto.ApiResponse;
import com.sushil.dto.ApiResponse.ApiError;
import com.sushil.exception.AppExceptions.*;
import com.sushil.exception.AppExceptions.MiningException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid value",
                        (first, second) -> first));

        log.warn("[EX] Validation failed on '{}': {}", req.getRequestURI(), fieldErrors);
        return respond(HttpStatus.BAD_REQUEST, req,
                ApiError.of("VALIDATION_FAILED", "Request validation failed", fieldErrors));
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            RuntimeException ex, HttpServletRequest req) {

        log.warn("[EX] BadRequest on '{}': {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, req, ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtp(
            OtpException ex, HttpServletRequest req) {

        log.warn("[EX] OTP failure on '{}': {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, req, ApiError.of("OTP_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        String msg = "Parameter '%s' has invalid value: %s".formatted(ex.getName(), ex.getValue());
        log.warn("[EX] TypeMismatch on '{}': {}", req.getRequestURI(), msg);
        return respond(HttpStatus.BAD_REQUEST, req, ApiError.of("TYPE_MISMATCH", msg));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest req) {

        String msg = "Required header '%s' is missing".formatted(ex.getHeaderName());
        log.warn("[EX] MissingHeader on '{}': {}", req.getRequestURI(), msg);
        return respond(HttpStatus.BAD_REQUEST, req, ApiError.of("MISSING_HEADER", msg));
    }

    // ── 401 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {

        log.warn("[EX] BadCredentials on '{}'", req.getRequestURI());
        return respond(HttpStatus.UNAUTHORIZED, req,
                ApiError.of("INVALID_CREDENTIALS", "Username or password is incorrect"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex, HttpServletRequest req) {
        log.warn("[EX] AccountDisabled on '{}'", req.getRequestURI());
        return respond(HttpStatus.UNAUTHORIZED, req, ApiError.of("ACCOUNT_DISABLED", "Your account has been disabled"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex, HttpServletRequest req) {
        log.warn("[EX] AccountLocked on '{}'", req.getRequestURI());
        return respond(HttpStatus.UNAUTHORIZED, req, ApiError.of("ACCOUNT_LOCKED", "Your account is locked"));
    }

    @ExceptionHandler({InvalidTokenException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuth(RuntimeException ex, HttpServletRequest req) {
        log.warn("[EX] Auth failure on '{}': {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.UNAUTHORIZED, req, ApiError.of("UNAUTHORIZED", "Authentication required"));
    }

    // ── 403 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler({AccessDeniedException.class, UnauthorizedException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException ex, HttpServletRequest req) {
        log.warn("[EX] Forbidden on '{}': {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.FORBIDDEN, req,
                ApiError.of("ACCESS_DENIED", "You do not have permission to access this resource"));
    }

    // ── 404 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {

        log.warn("[EX] NotFound on '{}': {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, req, ApiError.of("NOT_FOUND", ex.getMessage()));
    }

    // ── 409 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest req) {

        log.warn("[EX] Conflict on '{}': {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.CONFLICT, req, ApiError.of("DUPLICATE_RESOURCE", ex.getMessage()));
    }

    // ── 500 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(MiningException.class)
    public ResponseEntity<ApiResponse<Void>> handleMining(MiningException ex, HttpServletRequest req) {
        log.error("[EX] MiningFailed on '{}': {}", req.getRequestURI(), ex.getMessage(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, req, ApiError.of("MINING_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("[EX] Unhandled {} on '{}': {}", ex.getClass().getSimpleName(), req.getRequestURI(), ex.getMessage(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, req,
                ApiError.of("INTERNAL_ERROR", "An unexpected error occurred. Please try again later."));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static ResponseEntity<ApiResponse<Void>> respond(
            HttpStatus status, HttpServletRequest req, ApiError error) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(error, status, req.getRequestURI()));
    }
}
