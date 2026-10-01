package com.aptigen.contest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyContestRepository extends JpaRepository<WeeklyContest, Long> {
    List<WeeklyContest> findByStatusOrderByScheduledDateDesc(ContestStatus status);
    Optional<WeeklyContest> findTopByOrderByScheduledDateDesc();
    List<WeeklyContest> findByScheduledDateAndStatus(LocalDate scheduledDate, ContestStatus status);
    List<WeeklyContest> findAllByStatusInOrderByScheduledDateDesc(List<ContestStatus> statuses);
}