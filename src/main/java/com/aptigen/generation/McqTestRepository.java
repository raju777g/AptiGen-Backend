package com.aptigen.generation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McqTestRepository extends JpaRepository<McqTest, Long> {
    List<McqTest> findByOwnerUserId(Long ownerUserId);
    List<McqTest> findByIsPublicTrue();
    List<McqTest> findByIsMockTestTrue();
    List<McqTest> findByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
