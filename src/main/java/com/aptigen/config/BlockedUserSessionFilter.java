package com.aptigen.config;

import com.aptigen.user.User;
import com.aptigen.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class BlockedUserSessionFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authenticatedEmail(authentication);
        User user = email == null ? null : userRepository.findByEmail(email).orElse(null);
        if (user != null && user.isBlocked()) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
            String message = user.getBlockMessage() == null || user.getBlockMessage().isBlank()
                    ? "This account is blocked." : user.getBlockMessage();
            message += " Please email support@aptigen.com for help.";
            String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\r", "\\r").replace("\n", "\\n");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"" + escaped + "\",\"blocked\":true}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String authenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails details) return details.getUsername();
        if (principal instanceof OAuth2User oauthUser) return oauthUser.getAttribute("email");
        return null;
    }
}
