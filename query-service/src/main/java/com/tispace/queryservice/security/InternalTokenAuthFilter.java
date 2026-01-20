package com.tispace.queryservice.security;

import com.tispace.queryservice.config.InternalSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Filter to authenticate internal API requests using API Key header.
 * Only applies to paths starting with /internal/
 * Returns 401 if header is missing, 403 if token is invalid.
 * Uses constant-time comparison to prevent timing attacks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalTokenAuthFilter extends OncePerRequestFilter {

    private final InternalSecurityProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // фильтр нужен только для /internal/**
        if (!requestPath.startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // если токен не настроен — это ошибка конфигурации, лучше сразу 500
        if (!StringUtils.hasText(properties.getToken())) {
            log.error("Internal token is NOT configured! Internal API is blocked.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Internal API token is not configured\"}");
            return;
        }

        String providedToken = request.getHeader(properties.getHeader());

        if (!StringUtils.hasText(providedToken)) {
            log.warn("Missing {} header for internal endpoint: {}", properties.getHeader(), requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized: Missing authentication header\"}");
            return;
        }

        if (!isTokenValid(providedToken)) {
            log.warn("Invalid token for internal endpoint: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden: Invalid authentication token\"}");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private boolean isTokenValid(String providedToken) {
        byte[] expected = properties.getToken().getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }
}

