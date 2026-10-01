package com.aptigen.support;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);
}