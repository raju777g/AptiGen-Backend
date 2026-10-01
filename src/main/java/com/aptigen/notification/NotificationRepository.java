package com.aptigen.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdIsNullOrderByCreatedAtDesc();
    long countByUserIdAndIsReadFalse(Long userId);
}