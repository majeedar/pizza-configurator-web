package com.pizzaconfig.orderservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Per CLAUDE.md §15: include order_id in the MDC wherever a request touches an order,
// so it shows up alongside trace_id (added automatically by Micrometer Tracing) in every
// structured log line for that request.
@Component
public class OrderIdMdcFilter extends OncePerRequestFilter {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("/v1/orders/([0-9a-fA-F-]{36})");
    private static final String MDC_KEY = "order_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Matcher matcher = ORDER_ID_PATTERN.matcher(request.getRequestURI());
        try {
            if (matcher.find()) {
                MDC.put(MDC_KEY, matcher.group(1));
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
