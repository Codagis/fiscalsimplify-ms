package com.fiscalimplify.fiscalimplify.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Registra cada requisição HTTP no stdout (visível nos Deploy Logs do Railway).
 */
@Component
@Profile({"railway", "homolog"})
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@Slf4j
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return RailwayHealthFilter.isHealthPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String target = query != null ? uri + "?" + query : uri;
        Exception failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            failure = ex;
            throw ex;
        } finally {
            int status = response.getStatus();
            long ms = System.currentTimeMillis() - start;
            if (failure != null) {
                log.error("HTTP {} {} -> ERRO após {}ms: {}", method, target, ms, failure.toString(), failure);
            } else if (status >= 500) {
                log.error("HTTP {} {} -> {} ({}ms)", method, target, status, ms);
            } else if (status >= 400) {
                log.warn("HTTP {} {} -> {} ({}ms)", method, target, status, ms);
            } else {
                log.debug("HTTP {} {} -> {} ({}ms)", method, target, status, ms);
            }
        }
    }
}
