package com.aptigen.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {
    List<UserActivityEvent> findByUserIdOrderByOccurredAtDesc(Long userId);
    List<UserActivityEvent> findByOccurredAtBetweenOrderByOccurredAtAsc(LocalDateTime from, LocalDateTime to);
}
