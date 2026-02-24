package io.provenly.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the Auth Service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Configure security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless API
                .csrf(AbstractHttpConfigurer::disable)
                
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Stateless session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Configure authorization
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/web3/challenge",
                                "/api/v1/auth/web3/verify",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Password encoder bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins in development (configure properly for production)
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow common headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        
        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

