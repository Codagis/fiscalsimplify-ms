package com.fiscalimplify.fiscalimplify.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Responde health check do Railway antes de Security (sem depender de DB ou API key).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RailwayHealthFilter extends OncePerRequestFilter {

    private static final String BODY = "{\"status\":\"UP\"}";
    private static final Set<String> EXACT_PATHS = Set.of(
            "/health",
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isHealthPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(BODY);
    }

    static boolean isHealthPath(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        int q = uri.indexOf('?');
        String path = q >= 0 ? uri.substring(0, q) : uri;
        return EXACT_PATHS.contains(path) || path.endsWith("/health");
    }
}
