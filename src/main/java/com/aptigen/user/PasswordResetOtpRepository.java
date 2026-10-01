package com.aptigen.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    List<PasswordResetOtp> findByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}