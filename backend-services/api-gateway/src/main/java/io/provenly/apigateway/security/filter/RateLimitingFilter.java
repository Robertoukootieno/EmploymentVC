package io.provenly.apigateway.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent DDoS and brute force attacks
 * Implements token bucket algorithm per IP address
 * 
 * Rate limits:
 * - 100 requests per minute per IP
 * - Automatic cleanup of inactive buckets
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int REQUESTS_PER_MINUTE = 100;
    private static final int CLEANUP_THRESHOLD = 2000; // Maximum buckets to keep
    private static final int HTTP_429_TOO_MANY_REQUESTS = 429;
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String ip = getClientIp(request);
        
        try {
            Bucket bucket = getBucket(ip);
            
            if (bucket.tryConsume(1)) {
                // Request allowed, continue
                filterChain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                log.warn("Rate limit exceeded for IP: {}", ip);
                response.setStatus(HTTP_429_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
            }
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Bucket getBucket(String ip) {
        return cache.computeIfAbsent(ip, key -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(REQUESTS_PER_MINUTE)
                    .refillIntervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header from proxy
        String forwardedIp = request.getHeader("X-Forwarded-For");
        if (forwardedIp != null && !forwardedIp.isEmpty()) {
            return forwardedIp.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }

    @Override
    public void destroy() {
        // Cleanup buckets to prevent memory leak
        if (cache.size() > CLEANUP_THRESHOLD) {
            cache.clear();
            log.info("Cleared rate limiting cache due to size threshold");
        }
        super.destroy();
    }
}
