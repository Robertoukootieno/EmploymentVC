package io.provenly.apigateway.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Security headers filter
 * Adds standard security headers to all responses
 * 
 * Headers added:
 * - X-Content-Type-Options: Prevents MIME type sniffing
 * - X-Frame-Options: Prevents clickjacking
 * - X-XSS-Protection: XSS protection (legacy)
 * - Referrer-Policy: Controls referrer information
 * - Content-Security-Policy: Prevents injection attacks
 * - Strict-Transport-Security: Forces HTTPS
 * - Permissions-Policy: Controls browser features
 */
@Slf4j
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/actuator",
        "/health",
        "/metrics"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Add security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        
        // Content Security Policy - strict but practical
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");
        
        // HSTS - only for HTTPS (set only if request is secure)
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
}
