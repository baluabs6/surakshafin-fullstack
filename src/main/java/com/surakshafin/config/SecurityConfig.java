package com.surakshafin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Security gap fix: origins were hardcoded to "http://localhost:*", which is fine for the
    // demo but would either break or (if loosened carelessly) over-permit a real deployment.
    // Now driven by config so prod sets its real origin(s) via SURAKSHAFIN_CORS_ORIGINS.
    private final List<String> allowedOrigins;

    // Security gap fix: H2 console and Swagger/OpenAPI were always reachable, including in a
    // prod-like deployment of this exact jar. Now off by default and opt-in for local dev only.
    private final boolean devToolsEnabled;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                           @Value("${surakshafin.cors.allowed-origins}") String allowedOrigins,
                           @Value("${surakshafin.dev-tools-enabled:false}") boolean devToolsEnabled) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        this.devToolsEnabled = devToolsEnabled;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API; no cookie-based session to protect
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/v1/auth/**",
                            "/api/v1/literacy/**"
                    ).permitAll();
                    if (devToolsEnabled) {
                        auth.requestMatchers("/h2-console/**", "/docs/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll();
                    }
                    // Feature gap fix: status transitions on fraud reports / grievances used to have
                    // no endpoint at all; now that they exist, only platform operators can call them.
                    auth.requestMatchers("/api/v1/fraud/reports/*/status").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/grievances/*/status").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .headers(headers -> {
                    if (devToolsEnabled) {
                        headers.frameOptions(frame -> frame.disable()); // needed for h2-console only
                    }
                    // Security gap fix: no security response headers were set at all.
                    headers
                            .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                            .contentTypeOptions(contentType -> {}) // X-Content-Type-Options: nosniff
                            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfigurationFor("/**", config);
        return source;
    }
}
