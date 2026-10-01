package com.aptigen.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByUserIdAndSubmittedAtIsNotNull(Long userId);
    List<TestAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

}
