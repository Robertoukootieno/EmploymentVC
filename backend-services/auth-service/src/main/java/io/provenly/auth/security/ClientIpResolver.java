package io.provenly.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility to resolve client IP address from request
 * Handles proxy scenarios (X-Forwarded-For, X-Real-IP)
 */
@Slf4j
@Component
public class ClientIpResolver {

    /**
     * Get client IP from current HTTP request
     * Considers proxy headers: X-Forwarded-For, X-Real-IP, CF-Connecting-IP
     */
    public String getClientIp() {
        try {
            final ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs == null) {
                log.debug("No request context available, returning unknown IP");
                return "unknown";
            }

            final HttpServletRequest request = attrs.getRequest();
            return extractClientIp(request);
        } catch (Exception e) {
            log.warn("Error resolving client IP: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Get client IP from explicit HttpServletRequest
     */
    public String getClientIp(HttpServletRequest request) {
        return extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (contains original client IP when behind proxy)
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one (original client)
            final String clientIp = xForwardedFor.split(",")[0].trim();
            log.debug("Client IP from X-Forwarded-For: {}", clientIp);
            return clientIp;
        }

        // Check X-Real-IP header (alternative proxy header)
        final String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            log.debug("Client IP from X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        // Check Cloudflare header
        final String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            log.debug("Client IP from CF-Connecting-IP: {}", cfConnectingIp);
            return cfConnectingIp;
        }

        // Fallback to direct connection IP
        final String remoteAddr = request.getRemoteAddr();
        log.debug("Client IP from RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }
}
