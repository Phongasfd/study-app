package com.namphong.backend.security;

import com.namphong.backend.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final OAuth2SuccessHandler successHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // save csrf token in cookie
                .ignoringRequestMatchers(
                        "/api/auth/**",
                        "/oauth2/**" // dont check csrf for these routes
                ));
        http.cors(cors -> {});
        http.authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/auth/me").authenticated().anyRequest().permitAll());

        http.exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                })
        ); // when user is not authenticated but call auth route, spring security will send 401
        http.oauth2Login(oauth2 -> oauth2.successHandler(successHandler));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        // add jwt filter before UsernamePasswordAuthenticationFilter
        return http.build();
    }
}
