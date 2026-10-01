package com.aptigen.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {
    public String extractEmail(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user was provided.");
        }
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("The signed-in account did not provide an email address.");
            }
            return email;
        }
        return authentication.getName();
    }
}
