package com.sushil.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sushil.config.AesEncryptionUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Order(1)
@RequiredArgsConstructor
public class DecryptionFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DecryptionFilter.class);

    static final String ENCRYPTED_TOKEN_HEADER = "X-Auth-Token";

    @Value("${app.encryption.enabled:false}")
    private boolean encryptionEnabled;

    private final AesEncryptionUtil aesUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String contentType = httpRequest.getContentType();

        // DEV — skip all decryption, pass through as-is
        if (!encryptionEnabled) {
            log.trace("[DECRYPT-FILTER] Encryption disabled (dev) — skipping for {} {}", method, uri);
            chain.doFilter(request, response);
            return;
        }

        boolean isWriteMethod = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
        boolean isJson = contentType != null && contentType.contains("application/json");

        // Always wrap every request — header decryption needed for ALL methods (GET, DELETE too)
        // Body decryption only for POST/PUT with JSON content type
        DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(
                httpRequest, aesUtil, objectMapper, isWriteMethod && isJson, uri);

        log.debug("[DECRYPT-FILTER] {} {} — bodyDecrypt={}", method, uri, isWriteMethod && isJson);
        chain.doFilter(wrapper, response);
    }

    // ── Wrapper ──────────────────────────────────────────────────────────────

    static class DecryptedRequestWrapper extends HttpServletRequestWrapper {

        private static final Logger log = LoggerFactory.getLogger(DecryptedRequestWrapper.class);

        private final byte[] body;
        private final String decryptedToken;   // plain JWT after decrypting X-Auth-Token

        DecryptedRequestWrapper(HttpServletRequest request, AesEncryptionUtil aesUtil,
                                ObjectMapper mapper, boolean decryptBody, String uri) throws IOException {
            super(request);

            // ── 1. Decrypt X-Auth-Token header → plain JWT ──────────────────
            String encryptedToken = request.getHeader(ENCRYPTED_TOKEN_HEADER);
            String resolvedToken = null;
            if (encryptedToken != null && !encryptedToken.isBlank()) {
                log.info("[DECRYPT-FILTER] [{}] Encrypted token header received | X-Auth-Token='{}'",
                        uri, truncate(encryptedToken, 60));
                try {
                    resolvedToken = aesUtil.decrypt(encryptedToken);
                    log.info("[DECRYPT-FILTER] [{}] Token header decrypted successfully | JWT='{}'",
                            uri, maskToken(resolvedToken));
                } catch (Exception ex) {
                    log.warn("[DECRYPT-FILTER] [{}] Token header decryption failed: {}", uri, ex.getMessage());
                }
            }
            this.decryptedToken = resolvedToken;

            // ── 2. Decrypt request body ──────────────────────────────────────
            byte[] rawBytes = request.getInputStream().readAllBytes();
            byte[] resolved = rawBytes;

            if (decryptBody && rawBytes.length > 0) {
                String raw = new String(rawBytes, StandardCharsets.UTF_8).trim();
                log.info("[DECRYPT-FILTER] [{}] Encrypted request body received | size={} bytes | body='{}'",
                        uri, rawBytes.length, truncate(raw, 200));
                try {
                    JsonNode node = mapper.readTree(raw);
                    if (node.size() == 1 && node.has("payload")) {
                        String encryptedPayload = node.get("payload").asText();
                        log.debug("[DECRYPT-FILTER] [{}] Payload envelope detected | encrypted='{}'",
                                uri, truncate(encryptedPayload, 80));
                        String decrypted = aesUtil.decrypt(encryptedPayload);
                        resolved = decrypted.getBytes(StandardCharsets.UTF_8);
                        log.info("[DECRYPT-FILTER] [{}] Body decrypted | {} bytes → {} bytes | body='{}'",
                                uri, rawBytes.length, resolved.length, truncate(decrypted, 200));
                    } else {
                        log.debug("[DECRYPT-FILTER] [{}] No payload envelope — raw body passed unchanged", uri);
                    }
                } catch (Exception ex) {
                    log.warn("[DECRYPT-FILTER] [{}] Body decryption skipped: {}", uri, ex.getMessage());
                }
            }
            this.body = resolved;
        }

        // ── Override getHeader — expose decrypted JWT as Authorization ───────
        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name) && decryptedToken != null) {
                return "Bearer " + decryptedToken;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Authorization".equalsIgnoreCase(name) && decryptedToken != null) {
                return Collections.enumeration(List.of("Bearer " + decryptedToken));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // Inject Authorization into header names if decryptedToken is present
            if (decryptedToken != null) {
                List<String> names = Collections.list(super.getHeaderNames());
                if (names.stream().noneMatch(n -> n.equalsIgnoreCase("Authorization"))) {
                    names.add("Authorization");
                }
                return Collections.enumeration(names);
            }
            return super.getHeaderNames();
        }

        // ── Override getInputStream / getReader for body ─────────────────────
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                public int read() { return bis.read(); }
                public boolean isFinished() { return bis.available() == 0; }
                public boolean isReady() { return true; }
                public void setReadListener(ReadListener l) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        // ── Helpers ──────────────────────────────────────────────────────────
        private static String truncate(String s, int max) {
            if (s == null) return "null";
            return s.length() > max ? s.substring(0, max) + "...[truncated]" : s;
        }

        private static String maskToken(String token) {
            if (token == null) return "null";
            return token.length() > 20 ? token.substring(0, 20) + "...[JWT]" : token;
        }
    }
}
