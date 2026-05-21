package com.fiscalimplify.fiscalimplify.config;

import com.fiscalimplify.fiscalimplify.security.ApiKeyAuthenticationFilter;
import com.fiscalimplify.fiscalimplify.security.WebhookSecretAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuração de segurança do Fiscal Simplify.
 * API Key para endpoints protegidos, Webhook Secret para callbacks.
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-03-05
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${fiscalimplify.api.key:}")
    private String apiKey;

    @Value("${fiscalimplify.webhook.secret:}")
    private String webhookSecret;

    @Value("${fiscalimplify.security.disable:false}")
    private boolean securityDisabled;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain healthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/health", "/actuator/**", "/actuator/health/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain webhookSecurityFilterChain(HttpSecurity http) throws Exception {
        if (securityDisabled || !org.springframework.util.StringUtils.hasText(webhookSecret)) {
            return http.securityMatcher("/webhook/**").authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable).build();
        }

        http
                .securityMatcher("/webhook/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("ROLE_WEBHOOK"))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new WebhookSecretAuthenticationFilter(webhookSecret),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Webhook secret inválido ou ausente\"}");
                }));

        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        if (securityDisabled || !org.springframework.util.StringUtils.hasText(apiKey)) {
            return http.securityMatcher("/companies/**", "/nfce/**", "/nfe/**")
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable).build();
        }

        http
                .securityMatcher("/companies/**", "/nfce/**", "/nfe/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("ROLE_API_CLIENT"))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .headers(h -> h
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .frameOptions(f -> f.sameOrigin())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKey), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"API Key inválida ou ausente. Use o header X-API-Key ou Authorization: ApiKey {key}\"}");
                }));

        return http.build();
    }

    @Bean
    public SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(List.of("X-Request-Id", "X-Response-Time"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
