package com.aptigen.support;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupportChatRepository extends JpaRepository<SupportChat, Long> {
    List<SupportChat> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SupportChat> findByIdAndUserId(Long id, Long userId);
    List<SupportChat> findAllByOrderByCreatedAtDesc();
}
