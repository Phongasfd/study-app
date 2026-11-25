package com.namphong.backend.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.security.Principal;
import java.util.UUID;

/** The JwtFilter puts the user id in the principal; every /api route except auth requires it. */
final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(Principal principal) {
        if (principal == null) {
            // unreachable behind SecurityConfig (deny by default), kept as a guard
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        return UUID.fromString(principal.getName());
    }
}
