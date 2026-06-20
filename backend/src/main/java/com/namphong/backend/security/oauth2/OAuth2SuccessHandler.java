package com.namphong.backend.security.oauth2;

import com.namphong.backend.entity.AccountEntity;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.AccountRepository;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.security.JwtService;
import com.namphong.backend.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.redirect-uri}")
    private String redirectUrl;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String username = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            // if cannot find user -> run the code in lambda to creat user
            // orElse(value) luon tao value trc (eager)
            // orElseGet() chi chay khi can (lazy)
            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .username(username)
                    .build();
            UserEntity savedUser = userRepository.save(newUser);
            return savedUser;
        });

        // check account exist
        boolean accountExists = accountRepository.existsByProviderAndProviderAccountId("google", googleId);

        if (!accountExists) {
            AccountEntity account = new AccountEntity();
            account.setProvider("google");
            account.setProviderAccountId(googleId);
            account.setUser(user);

            accountRepository.save(account);
        }


        // create JWT
        String token = jwtService.generateToken(user.getId());

        // set access_token cookie
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);
        response.addCookie(cookie);

        // create and set refresh_token cookie
        String refresh = refreshTokenService.createRefreshTokenFor(user, 30); // 30 days
        Cookie refreshCookie = new Cookie("refresh_token", refresh);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/api/auth");
        refreshCookie.setMaxAge(30 * 24 * 60 * 60);
        response.addCookie(refreshCookie);

        response.sendRedirect(redirectUrl);
    }

}
