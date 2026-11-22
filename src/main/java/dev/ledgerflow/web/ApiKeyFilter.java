package dev.ledgerflow.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * Requires an X-API-Key header on /api/** when keys are configured.
 * With no keys configured (local development) the API is open.
 */
@Component
@Order(2)
public class ApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";

    private final List<byte[]> keys;

    public ApiKeyFilter(@Value("${ledgerflow.security.api-keys:}") String[] configuredKeys) {
        this.keys = Arrays.stream(configuredKeys)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .map(k -> k.getBytes(StandardCharsets.UTF_8))
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return keys.isEmpty() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented != null && matches(presented.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,\
                "detail":"Missing or invalid X-API-Key header","code":"unauthorized"}""");
    }

    /** Constant time comparison so response timing does not leak key prefixes. */
    private boolean matches(byte[] presented) {
        boolean ok = false;
        for (byte[] key : keys) {
            ok |= MessageDigest.isEqual(key, presented);
        }
        return ok;
    }
}
