package com.aptigen.attempt;

import com.aptigen.attempt.dto.*;
import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class TestAttemptController {

    private final TestAttemptService attemptService;
    private final UserService userService;
    private final AuthUtil authUtil;

    @PostMapping("/start")
    public ResponseEntity<?> start(Authentication auth, @RequestBody StartAttemptRequest request) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(attemptService.startAttempt(userId, request.testId()));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(Authentication auth, @PathVariable Long id, @RequestBody SubmitAttemptRequest request) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(attemptService.submitAttempt(userId, id, request.answers()));
    }
}
