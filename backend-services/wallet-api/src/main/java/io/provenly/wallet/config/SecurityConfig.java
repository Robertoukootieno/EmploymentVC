package io.provenly.wallet.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Security configuration for Wallet API.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/error"
                ).permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtSecret);
    }

    /**
     * JWT Authentication Filter.
     */
    @Slf4j
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final String jwtSecret;

        public JwtAuthenticationFilter(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            // Skip filter for public endpoints
            String path = request.getRequestURI();
            if (path.startsWith("/actuator") || 
                path.startsWith("/v3/api-docs") || 
                path.startsWith("/swagger-ui") ||
                path.equals("/error")) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String authHeader = request.getHeader("Authorization");
                
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("Missing or invalid Authorization header");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
                    return;
                }

                String token = authHeader.substring(7);
                
                // Validate JWT token
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                // Extract user ID from token
                String userIdStr = claims.getSubject();
                UUID userId = UUID.fromString(userIdStr);

                // Add user ID to request header for controllers
                request.setAttribute("X-User-Id", userId);
                
                // Also add as header for easier access
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request, userId);
                
                filterChain.doFilter(wrappedRequest, response);

            } catch (Exception e) {
                log.error("JWT validation failed", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            }
        }
    }

    /**
     * HTTP Servlet Request Wrapper to add user ID header.
     */
    private static class HttpServletRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final UUID userId;

        public HttpServletRequestWrapper(HttpServletRequest request, UUID userId) {
            super(request);
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equals(name)) {
                return userId.toString();
            }
            return super.getHeader(name);
        }
    }
}

