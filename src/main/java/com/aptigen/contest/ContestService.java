package com.aptigen.contest;

import com.aptigen.attempt.AttemptAnswerRepository;
import com.aptigen.attempt.TestAttempt;
import com.aptigen.attempt.TestAttemptRepository;
import com.aptigen.generation.McqTest;
import com.aptigen.generation.McqTestRepository;
import com.aptigen.notification.NotificationService;
import com.aptigen.wallet.TransactionType;
import com.aptigen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final WeeklyContestRepository contestRepository;
    private final ContestEntryRepository entryRepository;
    private final McqTestRepository testRepository;
    private final TestAttemptRepository attemptRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @Value("${aptigen.contest.default-entry-fee}")
    private int defaultEntryFee;

    @Transactional
    public WeeklyContest createWeeklyContest(Long testId, Integer entryFee) {
        McqTest test = testRepository.findById(testId).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalDate sunday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        if (sunday.equals(today)) sunday = sunday.plusWeeks(1);
        return contestRepository.save(WeeklyContest.builder().testId(test.getId()).scheduledDate(sunday)
                .entryFeeCoins(entryFee == null ? defaultEntryFee : Math.max(0, entryFee))
                .status(ContestStatus.UPCOMING).build());
    }

    @Scheduled(cron = "0 0 0 * * SUN", zone = "Asia/Kolkata")
    @Transactional
    public void activateWeeklyContests() {
        List<WeeklyContest> due = contestRepository.findByScheduledDateAndStatus(LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")), ContestStatus.UPCOMING);
        for (WeeklyContest contest : due) {
            contest.setStatus(ContestStatus.LIVE);
            contestRepository.save(contest);
        }
        if (!due.isEmpty()) notificationService.broadcastPlatformNews("Weekly Contest is Live!", "This week's contest is open. Good luck!");
    }
    public List<WeeklyContest> getUpcomingOrLive() {
        return contestRepository.findByStatusOrderByScheduledDateDesc(ContestStatus.LIVE);
    }

    @Transactional
    public ContestEntry enter(Long userId, Long contestId) {
        WeeklyContest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new IllegalArgumentException("Contest not found"));

        if (contest.getStatus() != ContestStatus.LIVE) {
            throw new IllegalStateException("This contest is not currently open for entry");
        }

        entryRepository.findByContestIdAndUserId(contestId, userId).ifPresent(e -> {
            throw new IllegalStateException("You have already entered this contest");
        });

        walletService.debit(userId, contest.getEntryFeeCoins(), TransactionType.CONTEST_ENTRY, contestId);

        return entryRepository.save(ContestEntry.builder()
                .contestId(contestId)
                .userId(userId)
                .build());
    }

    // Links a completed test attempt to the user's contest entry — called from
    // TestAttemptService once we wire that connection in the next sub-step
    @Transactional
    public void recordAttemptForContest(Long userId, Long contestId, Long attemptId) {
        ContestEntry entry = entryRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(() -> new IllegalStateException("No contest entry found for this user"));
        entry.setAttemptId(attemptId);
        entryRepository.save(entry);
    }

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Kolkata")
    @Transactional
    public void stopContestEntry() {
        for (WeeklyContest contest : contestRepository.findByStatusOrderByScheduledDateDesc(ContestStatus.LIVE)) {
            contest.setStatus(ContestStatus.CLOSED);
            contestRepository.save(contest);
        }
    }
    // Publishes contest results every Monday at 7:00 PM India time.
    @Scheduled(cron = "0 0 19 * * MON", zone = "Asia/Kolkata")
    @Transactional
    public void closeWeeklyContest() {
        List<WeeklyContest> liveContests = contestRepository.findByStatusOrderByScheduledDateDesc(ContestStatus.CLOSED);

        for (WeeklyContest contest : liveContests) {
            List<ContestEntry> entries = entryRepository.findByContestId(contest.getId());

            List<ContestEntry> scored = entries.stream()
                    .filter(e -> e.getAttemptId() != null)
                    .sorted(Comparator.comparing((ContestEntry e) -> getScore(e.getAttemptId())).reversed())
                    .toList();

            int[] prizes = {50, 30, 20}; // 1st, 2nd, 3rd place bonus coins
            for (int i = 0; i < scored.size(); i++) {
                ContestEntry entry = scored.get(i);
                entry.setRank(i + 1);
                entryRepository.save(entry);

                if (i < prizes.length) {
                    walletService.credit(entry.getUserId(), prizes[i], TransactionType.CONTEST_PRIZE, contest.getId());
                    notificationService.notifyUser(entry.getUserId(), "🏆 Contest Result",
                            "You finished #" + (i + 1) + " in this week's contest! " + prizes[i] + " bonus coins credited.");
                } else {
                    notificationService.notifyUser(entry.getUserId(), "Contest Result",
                            "You finished #" + (i + 1) + " in this week's contest. Better luck next week!");
                }
            }

            contest.setStatus(ContestStatus.COMPLETED);
            contestRepository.save(contest);
        }
    }

    public List<WeeklyContest> getAdminContests() {
        return contestRepository.findAllByStatusInOrderByScheduledDateDesc(List.of(ContestStatus.UPCOMING, ContestStatus.LIVE));
    }

    @Transactional
    public void cancelWeeklyContest(Long contestId) {
        WeeklyContest contest = contestRepository.findById(contestId).orElseThrow(() -> new IllegalArgumentException("Contest not found"));
        if (contest.getStatus() != ContestStatus.UPCOMING && contest.getStatus() != ContestStatus.LIVE) {
            throw new IllegalStateException("Only a scheduled or live contest can be removed");
        }
        List<ContestEntry> entries = entryRepository.findByContestId(contestId);
        for (ContestEntry entry : entries) walletService.credit(entry.getUserId(), contest.getEntryFeeCoins(), TransactionType.CONTEST_REFUND, contestId);
        entryRepository.deleteAll(entries);
        contestRepository.delete(contest);
    }

    public Long findContestIdByAttemptId(Long attemptId) {
        return entryRepository.findByAttemptId(attemptId).map(ContestEntry::getContestId).orElse(null);
    }

    public WeeklyContest requireReleasedAnswerKey(Long contestId) {
        WeeklyContest contest = contestRepository.findById(contestId).orElseThrow(() -> new IllegalArgumentException("Contest not found"));
        java.time.LocalDateTime releaseAt = contest.getScheduledDate().plusDays(1).atTime(19, 0);
        if (contest.getStatus() != ContestStatus.COMPLETED || java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).isBefore(releaseAt)) {
            throw new IllegalStateException("The contest answer key will be available Monday at 7:00 PM IST");
        }
        return contest;
    }

    public java.time.LocalDateTime answerKeyAvailableAt(Long contestId) {
        WeeklyContest contest = contestRepository.findById(contestId).orElseThrow(() -> new IllegalArgumentException("Contest not found"));
        return contest.getScheduledDate().plusDays(1).atTime(19, 0);
    }
    private int getScore(Long attemptId) {
        return attemptRepository.findById(attemptId).map(TestAttempt::getScore).orElse(0);
    }
}