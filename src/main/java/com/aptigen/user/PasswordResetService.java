package com.aptigen.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aptigen.email.EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void requestReset(String email) {
        // Don't reveal whether an email exists in the system — always respond the same way.
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

            PasswordResetOtp resetOtp = otpRepository.save(PasswordResetOtp.builder()
                    .userId(user.getId())
                    .otpHash(passwordEncoder.encode(otp))
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build());

            if (!emailService.sendPasswordResetOtp(user.getEmail(), otp)) {
                resetOtp.setUsed(true);
                otpRepository.save(resetOtp);
            }
        });
    }

    @Transactional
    public void verifyAndReset(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code or email"));

        List<PasswordResetOtp> candidates = otpRepository.findByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());

        PasswordResetOtp match = candidates.stream()
                .filter(o -> o.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(o -> passwordEncoder.matches(otp, o.getOtpHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        match.setUsed(true);
        otpRepository.save(match);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
