package com.aptigen.user;

import com.aptigen.user.dto.RegisterRequest;
import com.aptigen.wallet.TransactionType;
import com.aptigen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final ReferralRepository referralRepository;

    private final com.aptigen.email.EmailService emailService;
    private final com.aptigen.notification.NotificationService notificationService;

    @Value("${aptigen.admin.emails:}")
    private String adminEmails;

    public RegistrationResult register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getAuthProvider() != AuthProvider.LOCAL) {
                throw new IllegalArgumentException("Email already registered");
            }
            if (existing.isEmailVerified()) throw new IllegalArgumentException("Email already registered");
            issueLegacyVerificationCode(existing);
            return new RegistrationResult(existing.getEmail(), true);
        }

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pending == null) {
            pending = pendingRegistrationRepository.save(PendingRegistration.builder()
                    .email(email)
                    .name(request.getName())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .referredByCode(request.getReferralCode())
                    .otpHash(passwordEncoder.encode(generateVerificationCode()))
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build());
        }
        issuePendingVerificationCode(pending);
        return new RegistrationResult(pending.getEmail(), true);
    }

    public record RegistrationResult(String email, boolean verificationRequired) {}

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @org.springframework.transaction.annotation.Transactional
    public void verifyEmail(String email, String code) {
        email = normalizeEmail(email);
        code = code == null ? null : code.trim();
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pending != null) {
            verifyPendingRegistration(pending, code);
            return;
        }

        // Complete verification for accounts created before pending registrations were introduced.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
        if (user.isEmailVerified()) {
            return;
        }
        if (user.getVerificationCodeExpiresAt() == null || !user.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code expired. Request a new code.");
        }
        if (code == null || !code.matches("\\d{6}") || user.getVerificationToken() == null
                || !passwordEncoder.matches(code, user.getVerificationToken())) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        user.setEmailVerified(true);
        if (isConfiguredAdmin(user.getEmail())) user.setRole("ADMIN");
        user.setVerificationToken(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        walletService.credit(user.getId(), 50, TransactionType.SIGNUP_BONUS, null);
        emailService.sendWelcomeEmail(user.getEmail());

        notificationService.notifyUser(user.getId(), "Welcome to AptiGen 🎉", "Your 50 AG coin signup bonus has been credited.");

        // NEW: referral bonus — both sides get 20 coins, only once
        if (user.getReferredByUserId() != null) {
            referralRepository.findByRefereeUserId(user.getId())
                    .filter(r -> !r.isRewarded())
                    .ifPresent(referral -> {
                        walletService.credit(referral.getReferrerUserId(), 20, TransactionType.REFERRAL_BONUS, referral.getId());
                        walletService.credit(user.getId(), 20, TransactionType.REFERRAL_BONUS, referral.getId());
                        referral.setRewarded(true);
                        referralRepository.save(referral);
                    });
        }
    }

    public void resendVerificationCode(String email) {
        email = normalizeEmail(email);
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pending != null) {
            issuePendingVerificationCode(pending);
            return;
        }
        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified() && user.getAuthProvider() == AuthProvider.LOCAL)
                .ifPresent(this::issueLegacyVerificationCode);
    }

    private void issuePendingVerificationCode(PendingRegistration pending) {
        String code = generateVerificationCode();
        pending.setOtpHash(passwordEncoder.encode(code));
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pendingRegistrationRepository.save(pending);
        if (!emailService.sendVerificationOtpEmail(pending.getEmail(), code)) {
            throw new IllegalStateException("We couldn't send the verification code. Please try again.");
        }
    }

    private void issueLegacyVerificationCode(User user) {
        String code = generateVerificationCode();
        user.setVerificationToken(passwordEncoder.encode(code));
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        if (!emailService.sendVerificationOtpEmail(user.getEmail(), code)) {
            throw new IllegalStateException("We couldn't send the verification code. Please try again.");
        }
    }

    private String generateVerificationCode() {
        return String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void verifyPendingRegistration(PendingRegistration pending, String code) {
        if (pending.getExpiresAt() == null || !pending.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code expired. Request a new code.");
        }
        if (code == null || !code.matches("\\d{6}") || !passwordEncoder.matches(code, pending.getOtpHash())) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
        if (userRepository.existsByEmail(pending.getEmail())) {
            pendingRegistrationRepository.delete(pending);
            return;
        }

        User referrer = pending.getReferredByCode() == null || pending.getReferredByCode().isBlank()
                ? null : userRepository.findByReferralCode(pending.getReferredByCode()).orElse(null);
        User user = User.builder()
                .name(pending.getName())
                .email(pending.getEmail())
                .passwordHash(pending.getPasswordHash())
                .authProvider(AuthProvider.LOCAL)
                .referralCode(generateReferralCode())
                .referredByUserId(referrer == null ? null : referrer.getId())
                .emailVerified(true)
                .role(isConfiguredAdmin(pending.getEmail()) ? "ADMIN" : "USER")
                .build();
        User saved = userRepository.save(user);
        walletService.createWallet(saved.getId());
        if (referrer != null) {
            referralRepository.save(Referral.builder()
                    .referrerUserId(referrer.getId())
                    .refereeUserId(saved.getId())
                    .build());
        }
        pendingRegistrationRepository.delete(pending);

        walletService.credit(saved.getId(), 50, TransactionType.SIGNUP_BONUS, null);
        emailService.sendWelcomeEmail(saved.getEmail());
        notificationService.notifyUser(saved.getId(), "Welcome to AptiGen", "Your 50 AG coin signup bonus has been credited.");
        if (saved.getReferredByUserId() != null) {
            referralRepository.findByRefereeUserId(saved.getId())
                    .ifPresent(referral -> {
                        walletService.credit(referral.getReferrerUserId(), 20, TransactionType.REFERRAL_BONUS, referral.getId());
                        walletService.credit(saved.getId(), 20, TransactionType.REFERRAL_BONUS, referral.getId());
                        referral.setRewarded(true);
                        referralRepository.save(referral);
                    });
        }
    }

    private String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isConfiguredAdmin(String email) {
        if (email == null) return false;
        return java.util.Arrays.stream(adminEmails.split(",")).map(String::trim).anyMatch(configured -> configured.equalsIgnoreCase(email));
    }

    public record CheckInResult(boolean alreadyCheckedIn, int streak, int coinsAwarded, int newBalance) {}

    @org.springframework.transaction.annotation.Transactional
    public CheckInResult checkIn(String email) {
        User user = findByEmail(email);
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = user.getLastLoginDate();

        if (today.equals(lastLogin)) {
            int balance = walletService.getWallet(user.getId()).getBalance();
            return new CheckInResult(true, user.getCurrentStreak(), 0, balance);
        }

        if (lastLogin != null && lastLogin.equals(today.minusDays(1))) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
        }

        user.setLastLoginDate(today);
        userRepository.save(user);

        int reward = 2 + Math.min(user.getCurrentStreak(), 3);
        walletService.credit(user.getId(), reward, TransactionType.STREAK_BONUS, null);

        int newBalance = walletService.getWallet(user.getId()).getBalance();
        return new CheckInResult(false, user.getCurrentStreak(), reward, newBalance);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    public record ReferralStats(long friendsInvited, long successfulSignups, long pendingReferrals, long coinsEarned) {}

    public ReferralStats getReferralStats(Long userId) {
        List<Referral> referrals = referralRepository.findByReferrerUserId(userId);
        long total = referrals.size();
        long successful = referrals.stream().filter(Referral::isRewarded).count();
        long pending = total - successful;
        long coinsEarned = successful * 20; // matches the flat 20-coin referral bonus from Step 23
        return new ReferralStats(total, successful, pending, coinsEarned);
    }
}
