package com.pizzaconfig.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Downstream services never see or verify the JWT themselves (no service-to-service
// auth exists anywhere in this system — every internal service already implicitly
// trusts whatever the gateway forwards). Stamps the JWT subject onto the outgoing
// request as X-Customer-Id (scope=customer) or X-Staff-Id (scope=staff/admin), so
// order-service knows which account a request belongs to without ever trusting a
// client-supplied id in the body or path.
@Component
public class IdentityHeaderGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .ofType(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> withIdentityHeader(exchange, jwt))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    // request.mutate().header(...)/.headers(...) both throw UnsupportedOperationException
    // here — the incoming request's HttpHeaders is Spring's ReadOnlyHttpHeaders wrapper,
    // and neither mutate() path copies it into something mutable first in this version.
    // Decorating getHeaders() to build a fresh HttpHeaders instead sidesteps that entirely.
    private ServerWebExchange withIdentityHeader(ServerWebExchange exchange, Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        String headerName = switch (scope) {
            case "customer" -> "X-Customer-Id";
            case "staff", "admin" -> "X-Staff-Id";
            default -> null;
        };
        if (headerName == null) {
            return exchange;
        }

        ServerHttpRequest originalRequest = exchange.getRequest();
        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(originalRequest) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.set(headerName, jwt.getSubject());
                return headers;
            }
        };
        return exchange.mutate().request(decoratedRequest).build();
    }

    // Must run before NettyRoutingFilter (which also sits at LOWEST_PRECEDENCE) so the
    // mutated request — with the header attached — is what actually gets forwarded.
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
