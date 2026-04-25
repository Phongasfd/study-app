package com.namphong.backend.security.oauth2;

import com.namphong.backend.entity.AccountEntity;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.AccountRepository;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.security.JwtService;
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

        // set cookie
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        response.addCookie(cookie);

        response.sendRedirect("http://localhost:5173");
    }

}
