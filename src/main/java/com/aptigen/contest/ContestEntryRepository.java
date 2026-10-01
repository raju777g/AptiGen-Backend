package com.aptigen.contest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ContestEntryRepository extends JpaRepository<ContestEntry, Long> {
    List<ContestEntry> findByContestId(Long contestId);
    Optional<ContestEntry> findByContestIdAndUserId(Long contestId, Long userId);
    Optional<ContestEntry> findByAttemptId(Long attemptId);
    long countByUserId(Long userId);
}