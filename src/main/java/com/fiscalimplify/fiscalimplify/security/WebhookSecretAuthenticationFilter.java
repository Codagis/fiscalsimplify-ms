package com.fiscalimplify.fiscalimplify.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de autenticação para webhooks (header X-Webhook-Secret).
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-03-05
 */
@Slf4j
@RequiredArgsConstructor
public class WebhookSecretAuthenticationFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final String validWebhookSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String secret = request.getHeader(WEBHOOK_SECRET_HEADER);

        if (StringUtils.hasText(validWebhookSecret) && validWebhookSecret.equals(secret)) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "webhook-nuvemfiscal",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_WEBHOOK"))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
