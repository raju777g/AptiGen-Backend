package com.aptigen.user;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    java.util.Optional<Referral> findByRefereeUserId(Long refereeUserId);
    List<Referral> findByReferrerUserId(Long referrerUserId);
    List<Referral> findByReferrerUserIdOrRefereeUserId(Long referrerUserId, Long refereeUserId);
}
