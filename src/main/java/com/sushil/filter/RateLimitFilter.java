package com.sushil.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sushil.dto.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_LIMIT   = 10;  // 10 req / min on auth endpoints
    private static final int GLOBAL_LIMIT = 300; // 300 req / min elsewhere

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(req);
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(req.getRequestURI()));

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<?> body = ApiResponse.error(
                ApiResponse.ApiError.of("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."),
                HttpStatus.TOO_MANY_REQUESTS, req.getRequestURI());
            mapper.writeValue(res.getWriter(), body);
        }
    }

    private Bucket buildBucket(String uri) {
        int limit = uri.startsWith("/api/auth") ? AUTH_LIMIT : GLOBAL_LIMIT;
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(limit).refillIntervally(limit, Duration.ofMinutes(1)).build())
            .build();
    }

    private String resolveKey(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) ip = ip.split(",")[0].trim();
        else ip = req.getRemoteAddr();
        return ip + ":" + req.getRequestURI();
    }
}
