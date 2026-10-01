package com.aptigen.notification;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final AuthUtil authUtil;

    @GetMapping("/personal")
    public ResponseEntity<?> personal(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(notificationService.getPersonal(userId));
    }

    @GetMapping("/platform")
    public ResponseEntity<?> platform() {
        return ResponseEntity.ok(notificationService.getPlatform());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markRead(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    // TEMPORARY — lets you manually create test "From Us" announcements from Postman
    // without needing an admin role system yet. We'll lock this behind an admin check
    // once you add roles; flagging honestly that it's wide open for now.
    record BroadcastRequest(String title, String message) {}

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody BroadcastRequest request) {
        notificationService.broadcastPlatformNews(request.title(), request.message());
        return ResponseEntity.ok().build();
    }
}
