package com.sushil.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sushil.config.AesEncryptionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayloadEncryptionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PayloadEncryptionInterceptor.class);
    private static final String ENCRYPTED_TOKEN_HEADER = "X-Auth-Token";

    @Value("${app.encryption.enabled:false}")
    private boolean encryptionEnabled;

    private final AesEncryptionUtil aesUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("[ENCRYPT-INTERCEPTOR] preHandle — {} {} | encryptionEnabled={}",
                request.getMethod(), request.getRequestURI(), encryptionEnabled);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        String uri = request.getRequestURI();

        // DEV — skip all encryption, response goes out as plain JSON
        if (!encryptionEnabled) {
            log.trace("[ENCRYPT-INTERCEPTOR] Encryption disabled (dev) — skipping for {}", uri);
            if (response instanceof ContentCachingResponseWrapper wrapper) {
                wrapper.copyBodyToResponse();
            }
            return;
        }

        // ── 1. Encrypt Authorization response header → X-Auth-Token ─────────
        String authHeader = response.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String plainToken = authHeader.substring(7);
            log.info("[ENCRYPT-INTERCEPTOR] [{}] Plain Authorization response header detected | JWT='{}'",
                    uri, maskToken(plainToken));
            String encryptedToken = aesUtil.encrypt(plainToken);
            response.setHeader("Authorization", null);
            response.setHeader(ENCRYPTED_TOKEN_HEADER, encryptedToken);
            // Expose header to Angular (CORS)
            response.addHeader("Access-Control-Expose-Headers", ENCRYPTED_TOKEN_HEADER);
            log.info("[ENCRYPT-INTERCEPTOR] [{}] Token encrypted → X-Auth-Token='{}'",
                    uri, truncate(encryptedToken, 60));
        }

        // ── 2. Encrypt response body ─────────────────────────────────────────
        if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
            log.warn("[ENCRYPT-INTERCEPTOR] [{}] Response is not ContentCachingResponseWrapper — skipping", uri);
            return;
        }

        byte[] original = wrapper.getContentAsByteArray();
        if (original.length == 0) {
            log.debug("[ENCRYPT-INTERCEPTOR] [{}] Empty response body — nothing to encrypt", uri);
            return;
        }

        String plainJson = new String(original);
        log.info("[ENCRYPT-INTERCEPTOR] [{}] Plain response before encryption | size={} bytes | body='{}'",
                uri, original.length, truncate(plainJson, 200));

        String encrypted = aesUtil.encrypt(plainJson);
        byte[] encryptedBytes = objectMapper.writeValueAsBytes(Map.of("payload", encrypted));

        log.info("[ENCRYPT-INTERCEPTOR] [{}] Body encrypted | {} bytes → {} bytes | encrypted='{}'",
                uri, original.length, encryptedBytes.length, truncate(encrypted, 80));

        wrapper.resetBuffer();
        response.setContentType("application/json");
        response.setContentLength(encryptedBytes.length);
        wrapper.getResponse().getOutputStream().write(encryptedBytes);
        wrapper.copyBodyToResponse();

        log.debug("[ENCRYPT-INTERCEPTOR] [{}] Encrypted response written to client", uri);
    }

    private String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() > max ? s.substring(0, max) + "...[truncated]" : s;
    }

    private String maskToken(String token) {
        return token != null && token.length() > 20 ? token.substring(0, 20) + "...[JWT]" : token;
    }
}
