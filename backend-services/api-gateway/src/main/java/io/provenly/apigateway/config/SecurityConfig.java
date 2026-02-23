package io.provenly.apigateway.config;

import io.provenly.apigateway.security.filter.RateLimitingFilter;
import io.provenly.apigateway.security.filter.SecurityHeadersFilter;
import io.provenly.apigateway.security.filter.SizeLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security configuration for API Gateway
 * 
 * Configures:
 * - Request/response filters
 * - CORS policies
 * - Security headers
 * - Rate limiting
 * - Size limits
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(1); // Execute first
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<SizeLimitFilter> sizeLimitFilter(SizeLimitFilter filter) {
        FilterRegistrationBean<SizeLimitFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(2); // Execute second
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter(SecurityHeadersFilter filter) {
        FilterRegistrationBean<SecurityHeadersFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(3); // Execute third
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins("${cors.allowed-origins:http://localhost:3000}")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
