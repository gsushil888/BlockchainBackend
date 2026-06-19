package com.sushil.security;

import com.sushil.config.AesEncryptionUtil;
import com.sushil.config.CacheConfig;
import com.sushil.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX   = "Bearer ";
    private static final String AUTH_HEADER     = "Authorization";
    private static final String ENCRYPTED_TOKEN = "X-Auth-Token";

    private final JwtUtil               jwtUtil;
    private final UserDetailsService    userDetailsService;
    private final UserSessionRepository sessionRepository;
    private final AesEncryptionUtil     aesUtil;
    private final TokenActiveChecker    tokenActiveChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                String username = jwtUtil.extractUsername(token);
                if (StringUtils.hasText(username)
                        && SecurityContextHolder.getContext().getAuthentication() == null
                        && tokenActiveChecker.isActive(token)) {

                    UserDetails principal = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.isTokenValid(token, principal)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ex) {
                log.warn("[JWT] Token processing failed on '{}': {}", request.getRequestURI(), ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX))
            return authHeader.substring(BEARER_PREFIX.length());

        String encryptedToken = request.getHeader(ENCRYPTED_TOKEN);
        if (StringUtils.hasText(encryptedToken)) {
            try { return aesUtil.decrypt(encryptedToken); }
            catch (Exception ex) { log.warn("[JWT] X-Auth-Token decryption failed: {}", ex.getMessage()); }
        }
        return null;
    }

    /** Separate Spring-managed bean so @Cacheable proxy works correctly on it. */
    @Component
    @RequiredArgsConstructor
    public static class TokenActiveChecker {

        private final UserSessionRepository sessionRepository;

        @Cacheable(value = CacheConfig.TOKEN_ACTIVE, key = "#token")
        public boolean isActive(String token) {
            return sessionRepository.isTokenActive(token, LocalDateTime.now());
        }
    }
}
