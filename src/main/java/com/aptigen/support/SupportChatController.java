package com.aptigen.support;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportChatController {

    private final SupportChatService supportChatService;
    private final UserService userService;
    private final AuthUtil authUtil;

    record StartChatRequest(String subject, String message) {}
    record SendMessageRequest(String message) {}

    @PostMapping("/chats")
    public ResponseEntity<?> startChat(Authentication auth, @RequestBody StartChatRequest request) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(supportChatService.startChat(userId, request.subject(), request.message()));
    }

    @GetMapping("/chats")
    public ResponseEntity<?> myChats(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(supportChatService.getUserChats(userId));
    }

    @GetMapping("/chats/{id}/messages")
    public ResponseEntity<?> messages(Authentication auth, @PathVariable Long id) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(supportChatService.getMessages(id, userId));
    }

    @PostMapping(value = "/chats/{id}/messages", consumes = "multipart/form-data")
    public ResponseEntity<?> sendMessage(
            Authentication auth, @PathVariable Long id,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment
    ) throws IOException {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(supportChatService.sendMessage(id, userId, message, attachment));
    }
}
