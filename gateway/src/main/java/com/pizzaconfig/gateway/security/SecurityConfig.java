package com.pizzaconfig.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Public: /v1/auth/** (login + customer self-registration, see AuthController) and
// /actuator/** (K8s probes need to reach these unauthenticated). Locked:
// /v1/customer/** requires the "customer" scope — a customer must register/log in
// before reaching the ordering flow at all (CLAUDE.md §4's original "anonymous
// customer" model was superseded once account-based order history was added).
// /v1/admin/** requires the "admin" scope. /v1/kitchen/** (staff-facing KDS — not in
// the literal §4 text, a convention introduced when this scaffold was built) requires
// any authenticated caller, not a specific scope.
//
// JWT verification uses a shared HMAC secret rather than an OIDC issuer/JWKS endpoint
// because there is no real identity provider in this scaffold yet — swap jwtDecoder()
// for NimbusReactiveJwtDecoder built from an issuer-uri once a real IdP exists.
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        // Self-service password change needs a real identity (to derive
                        // X-Staff-Id from) — carved out ahead of the general /v1/auth/**
                        // permitAll rule below, same ordering requirement as the images rule.
                        .pathMatchers(HttpMethod.PUT, "/v1/auth/staff/password").authenticated()
                        .pathMatchers("/v1/auth/**").permitAll()
                        // Pizza photos are public static assets, not customer data — carved out
                        // ahead of the general customer-scope rule because a plain <img src>
                        // can't attach a Bearer token. Must precede /v1/customer/** below.
                        .pathMatchers("/v1/customer/catalog/images/**").permitAll()
                        .pathMatchers("/v1/customer/**").hasAuthority("SCOPE_customer")
                        .pathMatchers("/v1/admin/**").hasAuthority("SCOPE_admin")
                        .pathMatchers("/v1/kitchen/**").authenticated()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.signing-secret}") String signingSecret) {
        SecretKeySpec key = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    // Dev-only: allows the local Vite dev server (and a locally-built frontend
    // container) to call this gateway from a different origin.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "http://localhost:3001"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
