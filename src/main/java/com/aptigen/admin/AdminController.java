package com.aptigen.admin;

import com.aptigen.attempt.TestAttempt;
import com.aptigen.attempt.TestAttemptRepository;
import com.aptigen.generation.McqTest;
import com.aptigen.generation.McqTestRepository;
import com.aptigen.support.*;
import com.aptigen.user.*;
import com.aptigen.wallet.CoinTransaction;
import com.aptigen.wallet.CoinTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final McqTestRepository tests;
    private final TestAttemptRepository attempts;
    private final CoinTransactionRepository transactions;
    private final ReferralRepository referrals;
    private final UserActivityEventRepository activity;
    private final SupportChatRepository chats;
    private final SupportMessageRepository messages;
    private final SessionRegistry sessionRegistry;

    public record BlockRequest(boolean blocked, String message) {}
    public record StartChatRequest(Long userId) {}
    public record ReplyRequest(String message) {}
    public record UserRow(Long id, String name, String email, LocalDateTime createdAt, boolean blocked, String blockMessage) {}
    public record Daily(LocalDate date, long newUsers, long testsGenerated, long coinsSpent) {}
    public record Overview(long totalUsers, long activeUsers, long newUsers, long testsGenerated, long coinsSpent, List<Daily> daily) {}
    public record ChatRow(Long id, Long userId, String userName, String userEmail, String subject, ChatStatus status, LocalDateTime createdAt) {}

    @GetMapping("/overview")
    public Overview overview(@RequestParam(defaultValue = "7days") String period,
                             @RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate start = switch (period) {
            case "today" -> today;
            case "yesterday" -> today.minusDays(1);
            case "custom" -> from == null ? today : from;
            default -> today.minusDays(6);
        };
        LocalDate end = switch (period) {
            case "yesterday" -> today.minusDays(1);
            case "custom" -> to == null ? start : to;
            default -> today;
        };
        if (end.isBefore(start)) end = start;
        LocalDateTime lower = start.atStartOfDay(), upper = end.plusDays(1).atStartOfDay();
        List<User> allUsers = users.findAll();
        List<McqTest> rangeTests = tests.findByCreatedAtBetween(lower, upper);
        List<CoinTransaction> rangeCoins = transactions.findByCreatedAtBetween(lower, upper);
        Map<LocalDate, Long> newUsers = allUsers.stream().filter(u -> !u.getCreatedAt().isBefore(lower) && u.getCreatedAt().isBefore(upper)).collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> generated = rangeTests.stream().collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> spent = rangeCoins.stream().filter(t -> t.getAmount() < 0).collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate(), Collectors.summingLong(t -> -(long)t.getAmount())));
        List<Daily> daily = start.datesUntil(end.plusDays(1)).map(d -> new Daily(d, newUsers.getOrDefault(d, 0L), generated.getOrDefault(d, 0L), spent.getOrDefault(d, 0L))).toList();
        Map<Long, UserActivityEvent> latest = new HashMap<>();
        for (UserActivityEvent event : activity.findAll()) {
            UserActivityEvent current = latest.get(event.getUserId());
            if (current == null || event.getOccurredAt().isAfter(current.getOccurredAt())) latest.put(event.getUserId(), event);
        }
        LocalDateTime activeSince = LocalDateTime.now().minusMinutes(15);
        long active = latest.values().stream().filter(e -> "LOGIN".equals(e.getEventType()) && !e.getOccurredAt().isBefore(activeSince)).count();
        return new Overview(allUsers.size(), active, daily.stream().mapToLong(Daily::newUsers).sum(), daily.stream().mapToLong(Daily::testsGenerated).sum(), daily.stream().mapToLong(Daily::coinsSpent).sum(), daily);
    }

    @GetMapping("/users")
    public List<UserRow> searchUsers(@RequestParam(defaultValue = "") String query) {
        if (query.isBlank()) return List.of();
        return users.findTop30ByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(query, query).stream().map(this::row).toList();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> userDetails(@PathVariable Long id) {
        Optional<User> found = users.findById(id);
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        User user = found.get();
        List<Map<String, Object>> attemptsForUser = attempts.findByUserIdOrderByStartedAtDesc(id).stream().map(this::attemptRow).toList();
        List<Map<String, Object>> referralRows = referrals.findByReferrerUserIdOrRefereeUserId(id, id).stream().map(r -> {
            Long otherId = Objects.equals(r.getReferrerUserId(), id) ? r.getRefereeUserId() : r.getReferrerUserId();
            User other = users.findById(otherId).orElse(null);
            return Map.<String, Object>of("id", r.getId(), "refereeUserId", otherId, "refereeName", other == null ? "Unknown" : other.getName(), "refereeEmail", other == null ? "" : other.getEmail(), "relation", Objects.equals(r.getReferrerUserId(), id) ? "Invited" : "Referred by", "rewarded", r.isRewarded(), "rewardCoins", r.isRewarded() ? 20 : 0, "createdAt", r.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "createdAt", user.getCreatedAt(), "blocked", user.isBlocked(), "blockMessage", user.getBlockMessage() == null ? "" : user.getBlockMessage(),
                "loginHistory", activity.findByUserIdOrderByOccurredAtDesc(id), "coinTransactions", transactions.findByUserIdOrderByCreatedAtDesc(id), "testAttempts", attemptsForUser, "referrals", referralRows));
    }

    private Map<String, Object> attemptRow(TestAttempt attempt) {
        McqTest test = tests.findById(attempt.getTestId()).orElse(null);
        Map<String, Object> row = new HashMap<>();
        row.put("id", attempt.getId()); row.put("testId", attempt.getTestId()); row.put("testTitle", test == null ? "Deleted test" : test.getTitle());
        row.put("startedAt", attempt.getStartedAt()); row.put("submittedAt", attempt.getSubmittedAt()); row.put("score", attempt.getScore()); row.put("totalQuestions", attempt.getTotalQuestions());
        return row;
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<?> setBlocked(@PathVariable Long id, @RequestBody BlockRequest request) {
        Optional<User> found = users.findById(id);
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        User user = found.get();
        if ("ADMIN".equals(user.getRole())) return ResponseEntity.badRequest().body(Map.of("message", "Administrator accounts cannot be blocked here."));
        if (request.blocked() && (request.message() == null || request.message().isBlank())) return ResponseEntity.badRequest().body(Map.of("message", "Add a message for the blocked user."));
        user.setBlocked(request.blocked()); user.setBlockMessage(request.blocked() ? request.message().trim() : null); users.save(user);
        if (request.blocked()) revokeSessions(user.getEmail());
        return ResponseEntity.ok(row(user));
    }

    private void revokeSessions(String email) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String principalEmail = principal instanceof UserDetails details ? details.getUsername()
                    : principal instanceof OAuth2User oauthUser ? oauthUser.getAttribute("email") : null;
            if (principalEmail != null && principalEmail.equalsIgnoreCase(email)) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) session.expireNow();
            }
        }
    }

    @GetMapping("/chats")
    public List<ChatRow> allChats() {
        return chats.findAllByOrderByCreatedAtDesc().stream().map(chat -> {
            User u = users.findById(chat.getUserId()).orElse(null);
            return new ChatRow(chat.getId(), chat.getUserId(), u == null ? "Unknown user" : u.getName(), u == null ? "" : u.getEmail(), chat.getSubject(), chat.getStatus(), chat.getCreatedAt());
        }).toList();
    }

    @PostMapping("/chats")
    public ResponseEntity<?> startChat(@RequestBody StartChatRequest request) {
        if (request.userId() == null || !users.existsById(request.userId())) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));
        SupportChat chat = chats.findByUserIdOrderByCreatedAtDesc(request.userId()).stream().filter(c -> c.getStatus() == ChatStatus.OPEN).findFirst().orElseGet(() -> chats.save(SupportChat.builder().userId(request.userId()).subject("Admin support").build()));
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/chats/{id}/messages")
    public ResponseEntity<?> getChatMessages(@PathVariable Long id) {
        if (!chats.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(messages.findByChatIdOrderByCreatedAtAsc(id));
    }

    @PostMapping("/chats/{id}/messages")
    public ResponseEntity<?> reply(@PathVariable Long id, @RequestBody ReplyRequest request) {
        if (!chats.existsById(id)) return ResponseEntity.notFound().build();
        if (request.message() == null || request.message().isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "Message cannot be empty."));
        SupportMessage saved = messages.save(SupportMessage.builder().chatId(id).senderType(SenderType.ADMIN).message(request.message().trim()).build());
        return ResponseEntity.ok(saved);
    }

    private UserRow row(User user) { return new UserRow(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.isBlocked(), user.getBlockMessage()); }
}
