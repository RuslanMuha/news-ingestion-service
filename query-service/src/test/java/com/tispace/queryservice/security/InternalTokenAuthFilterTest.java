package com.tispace.queryservice.security;

import com.tispace.queryservice.config.InternalSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalTokenAuthFilterTest {

    @Mock
    private InternalSecurityProperties properties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipFilter_whenPathIsNotInternal() throws ServletException, IOException {
        var filter = new InternalTokenAuthFilter(properties);
        when(request.getRequestURI()).thenReturn("/api/public/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturn500_whenInternalTokenIsNotConfigured() throws ServletException, IOException {
        var filter = new InternalTokenAuthFilter(properties);
        when(request.getRequestURI()).thenReturn("/internal/test");

        when(properties.getToken()).thenReturn("   ");

        var sw = new StringWriter();
        doReturn(new PrintWriter(sw, true)).when(response).getWriter();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response).setContentType("application/json");
        assertEquals("{\"error\":\"Internal API token is not configured\"}", sw.toString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturn401_whenMissingAuthHeader() throws ServletException, IOException {
        var filter = new InternalTokenAuthFilter(properties);
        when(request.getRequestURI()).thenReturn("/internal/reports");

        when(properties.getToken()).thenReturn("EXPECTED_TOKEN");
        when(properties.getHeader()).thenReturn("X-Internal-Token");

        when(request.getHeader("X-Internal-Token")).thenReturn(null);

        var sw = new StringWriter();
        doReturn(new PrintWriter(sw, true)).when(response).getWriter();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertEquals("{\"error\":\"Unauthorized: Missing authentication header\"}", sw.toString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturn403_whenTokenIsInvalid() throws ServletException, IOException {
        var filter = new InternalTokenAuthFilter(properties);
        when(request.getRequestURI()).thenReturn("/internal/metrics");

        when(properties.getToken()).thenReturn("EXPECTED_TOKEN");
        when(properties.getHeader()).thenReturn("X-Internal-Token");

        when(request.getHeader("X-Internal-Token")).thenReturn("WRONG_TOKEN");

        var sw = new StringWriter();
        doReturn(new PrintWriter(sw, true)).when(response).getWriter();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        assertEquals("{\"error\":\"Forbidden: Invalid authentication token\"}", sw.toString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSetAuthenticationAndProceed_whenTokenIsValid() throws ServletException, IOException {
        var filter = new InternalTokenAuthFilter(properties);
        when(request.getRequestURI()).thenReturn("/internal/data");

        when(properties.getToken()).thenReturn("EXPECTED_TOKEN");
        when(properties.getHeader()).thenReturn("X-Internal-Token");

        when(request.getHeader("X-Internal-Token")).thenReturn("EXPECTED_TOKEN");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        verify(response, never()).setStatus(anyInt());
        verify(response, never()).setContentType(anyString());
        verify(response, never()).getWriter();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals("internal-service", auth.getPrincipal());

        Set<String> roles = auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("ROLE_INTERNAL"), roles);
    }
}