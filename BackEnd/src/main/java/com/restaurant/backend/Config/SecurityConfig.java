package com.restaurant.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Completely ignore security for WebSocket endpoints
        return (web) -> web.ignoring()
                .requestMatchers("/ws/**");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins for Cloudflare tunnels
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:*",
            "https://app.dewjunior.id.vn"

    ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        // Add specific headers that might be needed
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // Enable CORS for HTTP requests

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // IMPORTANT: WebSocket requests must bypass security completely
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").permitAll() // WebSocket - must be first
                        .requestMatchers("/api/**").permitAll() // API endpoints
                        .requestMatchers("/**").permitAll() // Allow all other requests
                )

                // Tắt login form & http basic
                .httpBasic(c -> c.disable())
                .formLogin(l -> l.disable())
                
                // Disable security headers that might interfere with WebSocket
                .headers(headers -> headers
                        .contentTypeOptions().disable()
                        .frameOptions().disable()
                        .xssProtection(xss -> xss.disable())
                )
                
                // Allow WebSocket connections - stateless session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                )

                // Disable request cache to allow WebSocket upgrade
                .requestCache(cache -> cache.disable());

        return http.build();
    }
}
