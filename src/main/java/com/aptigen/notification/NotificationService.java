package com.aptigen.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notifyUser(Long userId, String title, String message) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .build());
    }

    public void broadcastPlatformNews(String title, String message) {
        notificationRepository.save(Notification.builder()
                .userId(null)
                .title(title)
                .message(message)
                .build());
    }

    public List<Notification> getPersonal(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getPlatform() {
        return notificationRepository.findByUserIdIsNullOrderByCreatedAtDesc();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}