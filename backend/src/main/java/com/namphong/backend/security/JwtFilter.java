package com.namphong.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if(request.getCookies() != null){
            for (Cookie cookie : request.getCookies()) {
                if("access_token".equals(cookie.getName())) {
                    try {
                        UUID userId =  jwtService.getUserId(cookie.getValue());

                        // Create authentication object for spring security
                        // principal = userId, credentials = null, authorities = empty list
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());

                        // Save authentication in SecurityContext
                        // -Spring know this request logged in
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch(Exception e) {
                        // If token is invalid or expired, do not set the SecurityContext authentication.
                        // Let the filter chain continue so public endpoints (e.g. refresh) can be reached.
                        // Secured endpoints will be naturally blocked by Spring Security.
                    }
                    break;
                }
            }

        }
        // Call filter chain to continue the request
        // if not, request will be blocked here
        filterChain.doFilter(request, response);

    }
}
