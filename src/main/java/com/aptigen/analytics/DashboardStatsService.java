package com.aptigen.analytics;

import com.aptigen.attempt.TestAttempt;
import com.aptigen.attempt.TestAttemptRepository;
import com.aptigen.contest.ContestEntryRepository;
import com.aptigen.user.User;
import com.aptigen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final TestAttemptRepository attemptRepository;
    private final ContestEntryRepository contestEntryRepository;
    private final UserRepository userRepository;

    public record DashboardStats(
            int testsTaken, int averageScorePercent, long contestsJoined, int currentStreak, int questionsSolved
    ) {}

    public DashboardStats getStats(Long userId) {
        List<TestAttempt> attempts = attemptRepository.findByUserIdAndSubmittedAtIsNotNull(userId);

        int testsTaken = attempts.size();
        int averagePercent = 0;
        if (testsTaken > 0) {
            double totalPercent = attempts.stream()
                    .mapToDouble(a -> a.getTotalQuestions() == 0 ? 0 : (a.getScore() * 100.0) / a.getTotalQuestions())
                    .sum();
            averagePercent = (int) Math.round(totalPercent / testsTaken);
        }

        int questionsSolved = attempts.stream().mapToInt(TestAttempt::getTotalQuestions).sum();
        long contestsJoined = contestEntryRepository.countByUserId(userId);
        User user = userRepository.findById(userId).orElseThrow();

        return new DashboardStats(testsTaken, averagePercent, contestsJoined, user.getCurrentStreak(), questionsSolved);
    }


}