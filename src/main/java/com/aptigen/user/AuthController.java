package com.aptigen.user;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthUtil authUtil;

    private final PasswordResetService passwordResetService;

    record ResetRequestRequest(String email) {}
    record ResetVerifyRequest(String email, String otp, String newPassword) {}
    record VerifyEmailRequest(@NotBlank @Email String email, @NotBlank String code) {}
    record ResendVerificationRequest(@NotBlank @Email String email) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        UserService.RegistrationResult result = userService.register(request);
        return ResponseEntity.status(201).body(result);
    }

    record UserSummary(Long id, String name, String email, boolean emailVerified, String avatarUrl, String role) {}

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByEmail(authUtil.extractEmail(principal));
        return ResponseEntity.ok(new UserSummary(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified(), user.getAvatarUrl(), user.getRole()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.email(), request.code());
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/verification/resend")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerificationCode(request.email());
        return ResponseEntity.ok(Map.of("message", "If the account is awaiting verification, a new code has been sent."));
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(Authentication auth) {
        UserService.CheckInResult result = userService.checkIn(authUtil.extractEmail(auth));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/referral-link")
    public ResponseEntity<?> referralLink(Authentication auth) {
        User user = userService.findByEmail(authUtil.extractEmail(auth));
        return ResponseEntity.ok(new ReferralLinkResponse(user.getReferralCode()));
    }

    record ReferralLinkResponse(String referralCode) {}

    @GetMapping("/referral-stats")
    public ResponseEntity<?> referralStats(Authentication auth) {
        User user = userService.findByEmail(authUtil.extractEmail(auth));
        return ResponseEntity.ok(userService.getReferralStats(user.getId()));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ResetRequestRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset code has been sent."));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<?> verifyPasswordReset(@RequestBody ResetVerifyRequest request) {
        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        passwordResetService.verifyAndReset(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

}
