package com.aptigen.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    java.util.Optional<User> findByReferralCode(String referralCode);
    java.util.List<User> findTop30ByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(String name, String email);
    long countByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

}
