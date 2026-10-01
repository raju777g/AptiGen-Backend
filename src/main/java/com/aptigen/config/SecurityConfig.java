package com.aptigen.config;

import com.aptigen.user.CustomOAuth2UserService;
import com.aptigen.user.UserActivityService;
import com.aptigen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserActivityService userActivityService;
    private final UserRepository userRepository;
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://aptigenai.netlify.app",
                "https://01a0f743-edb3-77f0-8a1f-771bffe3879d-8080.eur-1.aiven.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private AuthenticationSuccessHandler oauthSuccessHandler() {
        return (request, response, authentication) -> {
            // Explicitly persist the OAuth authentication before redirecting.
            // This is required when SecurityContextHolderFilter is active and a
            // custom success handler is used.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
                String email = oauthUser.getAttribute("email");
                if (email != null) {
                    userRepository.findByEmail(email).ifPresent(user -> userActivityService.record(user.getId(), "LOGIN"));
                }
            }
            String destination = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ADMIN".equals(authority.getAuthority())) ? "/admin" : "/dashboard";
            response.sendRedirect(frontendUrl + destination);
        };
    }

    private AuthenticationFailureHandler oauthFailureHandler() {
        return (request, response, exception) -> response.sendRedirect(frontendUrl + "/login?error=oauth");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                .cors(cors -> {})
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry))
                .addFilterAfter(new BlockedUserSessionFilter(userRepository), org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register", "/api/auth/login", "/api/auth/verify", "/api/auth/verification/resend",
                                "/api/payments/webhook", "/oauth2/**", "/login/oauth2/**",
                                "/api/auth/password-reset/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> {
                            userRepository.findByEmail(auth.getName()).ifPresent(user -> userActivityService.record(user.getId(), "LOGIN"));
                            res.setStatus(200);
                        })
                        .failureHandler((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            String username = req.getParameter("username");
                            var blocked = username == null ? java.util.Optional.<com.aptigen.user.User>empty() : userRepository.findByEmail(username).filter(com.aptigen.user.User::isBlocked);
                            String message = blocked.map(u -> (u.getBlockMessage() == null || u.getBlockMessage().isBlank() ? "This account is blocked." : u.getBlockMessage()) + " Please email support@aptigen.com for help.").orElse("Invalid email or password.");
                            String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"")
                                    .replace("\r", "\\r").replace("\n", "\\n");
                            res.getWriter().write("{\"message\":\"" + escapedMessage + "\",\"blocked\":" + blocked.isPresent() + "}");
                        })
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oauthSuccessHandler())
                        .failureHandler(oauthFailureHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            if (auth != null) {
                                String email = auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser
                                        ? oauthUser.getAttribute("email") : auth.getName();
                                if (email != null) userRepository.findByEmail(email).ifPresent(user -> userActivityService.record(user.getId(), "LOGOUT"));
                            }
                            res.setStatus(200);
                        })
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
    
}
