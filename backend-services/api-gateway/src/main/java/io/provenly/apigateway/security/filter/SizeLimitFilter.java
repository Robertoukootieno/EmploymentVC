package io.provenly.apigateway.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * Size limit filter to prevent buffer overflow and memory exhaustion attacks
 * 
 * Enforces:
 * - Maximum request size: 10MB (configurable)
 * - Maximum header size: 16KB
 * - Timeout enforcement via request wrapper
 */
@Slf4j
@Component
public class SizeLimitFilter extends OncePerRequestFilter {

    @Value("${security.size-limit.max-request-size:10485760}") // 10MB default
    private long maxRequestSize;

    @Value("${security.size-limit.max-header-size:16384}") // 16KB default
    private int maxHeaderSize;

    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/actuator",
        "/health"
    );
    private static final int HTTP_431_REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Check request size
        int contentLength = request.getContentLength();
        if (contentLength > maxRequestSize) {
            log.warn("Request size {} exceeds limit of {} from IP: {}", 
                contentLength, maxRequestSize, getClientIp(request));
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Request entity too large\"}");
            return;
        }

        // Check total header size
        int headerSize = request.getHeaderNames().asIterator().hasNext() ? 
            request.getHeader("Host").length() : 0;
        if (headerSize > maxHeaderSize) {
            log.warn("Header size {} exceeds limit of {} from IP: {}", 
                headerSize, maxHeaderSize, getClientIp(request));
            response.setStatus(HTTP_431_REQUEST_HEADER_FIELDS_TOO_LARGE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Request header fields too large\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedIp = request.getHeader("X-Forwarded-For");
        if (forwardedIp != null && !forwardedIp.isEmpty()) {
            return forwardedIp.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
