package com.aptigen.contest;

import com.aptigen.common.AuthUtil;
import com.aptigen.attempt.TestAttemptService;
import com.aptigen.attempt.dto.StartAttemptResponse;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final TestAttemptService attemptService;
    private final UserService userService;
    private final ContestEntryRepository entryRepository;
    private final AuthUtil authUtil;

    @GetMapping("/live")
    public ResponseEntity<?> live() {
        return ResponseEntity.ok(contestService.getUpcomingOrLive());
    }

    @PostMapping("/{id}/enter")
    public ResponseEntity<?> enter(Authentication auth, @PathVariable Long id) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(contestService.enter(userId, id));
    }

    @PostMapping("/{contestId}/start-attempt")
    public ResponseEntity<?> startAttempt(Authentication auth, @PathVariable Long contestId, @RequestParam Long testId) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        StartAttemptResponse response = attemptService.startContestAttempt(userId, contestId, testId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<?> leaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(entryRepository.findByContestId(id));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && "ADMIN".equals(userService.findByEmail(authUtil.extractEmail(auth)).getRole());
    }

    @GetMapping("/{id}/answer-key")
    public ResponseEntity<?> answerKey(@PathVariable Long id) {
        return ResponseEntity.ok(attemptService.getContestAnswerKey(id));
    }

    @GetMapping("/admin")
    public ResponseEntity<?> adminList(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(java.util.Map.of("message", "Administrator access required"));
        return ResponseEntity.ok(contestService.getAdminContests());
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> remove(Authentication auth, @PathVariable Long id) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(java.util.Map.of("message", "Administrator access required"));
        contestService.cancelWeeklyContest(id);
        return ResponseEntity.noContent().build();
    }
    public record CreateContestRequest(Long testId, Integer entryFeeCoins) {}

    @PostMapping("/admin")
    public ResponseEntity<?> create(Authentication auth, @RequestBody CreateContestRequest request) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body(java.util.Map.of("message", "Administrator access required"));
        }
        return ResponseEntity.ok(contestService.createWeeklyContest(request.testId(), request.entryFeeCoins()));
    }
}
