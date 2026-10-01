package com.aptigen.user;

import com.aptigen.email.EmailService;
import com.aptigen.notification.NotificationService;
import com.aptigen.wallet.TransactionType;
import com.aptigen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${aptigen.admin.emails:}")
    private String adminEmails;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        String providerName = request.getClientRegistration().getRegistrationId(); // "google" or "github"

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            // GitHub doesn't always return email in the main attributes if the user's email is private;
            // for a portfolio project we just fail clearly here rather than making an extra API call
            // to GitHub's /user/emails endpoint — worth knowing this is a real limitation.
            throw new OAuth2AuthenticationException("Email not available from " + providerName + ". Please make your email public or use email/password signup.");
        }

        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && existingUser.isBlocked()) {
            String blockMessage = existingUser.getBlockMessage() == null || existingUser.getBlockMessage().isBlank()
                    ? "This account is blocked." : existingUser.getBlockMessage();
            throw new OAuth2AuthenticationException(blockMessage + " Contact support@aptigen.com for help.");
        }

        AuthProvider authProvider = providerName.equalsIgnoreCase("google") ? AuthProvider.GOOGLE : AuthProvider.GITHUB;
        String providerId = oAuth2User.getAttribute("sub") != null
                ? oAuth2User.getAttribute("sub")        // Google's unique id field
                : String.valueOf(oAuth2User.getAttribute("id")); // GitHub's unique id field

        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> { /* existing user — nothing to create, just let login proceed */ },
                () -> {
                    String name = oAuth2User.getAttribute("name");
                    User newUser = User.builder()
                            .name(name != null ? name : email.split("@")[0])
                            .email(email)
                            .role(isConfiguredAdmin(email) ? "ADMIN" : "USER")
                            .authProvider(authProvider)
                            .providerId(providerId)
                            .emailVerified(true) // OAuth providers already verified the email themselves
                            .referralCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .build();
                    User saved = userRepository.save(newUser);
                    walletService.createWallet(saved.getId());
                    walletService.credit(saved.getId(), 50, TransactionType.SIGNUP_BONUS, null);
                    emailService.sendWelcomeEmail(saved.getEmail());
                    notificationService.notifyUser(saved.getId(), "Welcome to AptiGen 🎉", "Your 50 AG coin signup bonus has been credited.");
                }
        );

        User authenticatedUser = userRepository.findByEmail(email).orElseThrow();
        Set<org.springframework.security.core.GrantedAuthority> authorities = new HashSet<>(oAuth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(authenticatedUser.getRole() == null ? "USER" : authenticatedUser.getRole()));
        String nameAttribute = request.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), nameAttribute);
    }

    private boolean isConfiguredAdmin(String email) {
        return java.util.Arrays.stream(adminEmails.split(",")).map(String::trim).anyMatch(configured -> configured.equalsIgnoreCase(email));
    }
}
