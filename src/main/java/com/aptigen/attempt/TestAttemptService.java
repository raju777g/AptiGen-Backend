package com.aptigen.attempt;

import com.aptigen.attempt.dto.*;
import com.aptigen.generation.McqQuestion;
import com.aptigen.generation.McqQuestionRepository;
import com.aptigen.generation.McqTest;
import com.aptigen.generation.McqTestRepository;
import com.aptigen.wallet.TransactionType;
import com.aptigen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestAttemptService {

    private final McqTestRepository testRepository;
    private final McqQuestionRepository questionRepository;
    private final TestAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final WalletService walletService;
    private final com.aptigen.contest.ContestService contestService;

    @Value("${aptigen.royalty-percentage}")
    private double royaltyPercentage;

    @Value("${aptigen.cashback-percentage}")
    private double cashbackPercentage;

    @Transactional
    public StartAttemptResponse startAttempt(Long userId, Long testId) {
        McqTest test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        List<McqQuestion> questions = questionRepository.findByTestId(testId);
        if (questions.isEmpty()) {
            throw new IllegalStateException("This test has no questions");
        }

        boolean isOwner = test.getOwnerUserId().equals(userId);
        int coinsToCharge = 0;

        if (!isOwner) {
            if (!test.isPublic()) {
                throw new IllegalStateException("This test is private");
            }
            coinsToCharge = questions.size(); // 1 coin per question, per your spec
            walletService.debit(userId, coinsToCharge, TransactionType.TEST_ATTEMPT_PAID, testId);
        }

        TestAttempt attempt = attemptRepository.save(TestAttempt.builder()
                .testId(testId)
                .userId(userId)
                .totalQuestions(questions.size())
                .coinsSpent(coinsToCharge)
                .build());

        List<QuestionForAttempt> questionDtos = questions.stream()
                .map(q -> new QuestionForAttempt(
                        q.getId(), q.getQuestionText(),
                        q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                // NOTE: correctOption intentionally excluded so the frontend can't see it before submitting
                .collect(Collectors.toList());

        return new StartAttemptResponse(attempt.getId(), questionDtos, test.getSecondsPerQuestion());
    }

    @Transactional
    public StartAttemptResponse startContestAttempt(Long userId, Long contestId, Long testId) {
        StartAttemptResponse response = startAttempt(userId, testId); // reuses all existing logic
        contestService.recordAttemptForContest(userId, contestId, response.attemptId());
        return response;
    }

    @Transactional
    public SubmitAttemptResponse submitAttempt(Long userId, Long attemptId, List<com.aptigen.attempt.dto.AnswerSubmission> answers) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));

        if (!attempt.getUserId().equals(userId)) {
            throw new IllegalStateException("This attempt does not belong to you");
        }
        if (attempt.getSubmittedAt() != null) {
            throw new IllegalStateException("This attempt was already submitted");
        }

        List<McqQuestion> questions = questionRepository.findByTestId(attempt.getTestId());
        McqTest test = testRepository.findById(attempt.getTestId())
                .orElseThrow(() -> new IllegalStateException("Test not found"));
                long elapsedSeconds = Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds();
        long totalTimeSeconds = (long) test.getSecondsPerQuestion() * attempt.getTotalQuestions();
        if (elapsedSeconds > totalTimeSeconds + 5L) {
            throw new IllegalStateException("The test time has expired");
        }

        List<AnswerSubmission> submissions = answers == null ? List.of() : answers;
        Map<Long, String> selectedByQuestion = submissions.stream()
                .filter(answer -> answer.questionId() != null && answer.selectedOption() != null)
                .collect(Collectors.toMap(AnswerSubmission::questionId, answer -> answer.selectedOption().toUpperCase(), (first, last) -> last));

        int correctCount = 0;
        for (McqQuestion question : questions) {
            String selectedOption = selectedByQuestion.get(question.getId());
            if (selectedOption == null) continue;
            boolean isCorrect = question.getCorrectOption() != null
                    && question.getCorrectOption().equalsIgnoreCase(selectedOption);
            if (isCorrect) correctCount++;
            answerRepository.save(AttemptAnswer.builder()
                    .attemptId(attemptId)
                    .questionId(question.getId())
                    .selectedOption(selectedOption)
                    .isCorrect(isCorrect)
                    .build());
        }

        attempt.setScore(correctCount);
        attempt.setSubmittedAt(LocalDateTime.now());

        boolean isPerfectScore = correctCount == attempt.getTotalQuestions();
        boolean cashbackAwarded = false;

        boolean isOwner = test.getOwnerUserId().equals(userId);

        if (!isOwner && attempt.getCoinsSpent() > 0) {
            // Accuracy cashback: 50% refund on a perfect score
            if (isPerfectScore) {
                int cashbackAmount = (int) Math.round(attempt.getCoinsSpent() * cashbackPercentage);
                if (cashbackAmount > 0) {
                    walletService.credit(userId, cashbackAmount, TransactionType.ACCURACY_CASHBACK, attemptId);
                    cashbackAwarded = true;
                }
            }

            // Creator royalty: owner earns a cut whenever someone else takes their public test
            int royaltyAmount = (int) Math.round(attempt.getCoinsSpent() * royaltyPercentage);
            if (royaltyAmount > 0) {
                walletService.credit(test.getOwnerUserId(), royaltyAmount, TransactionType.ROYALTY_EARNED, attemptId);
            }
        }

        attempt.setCashbackAwarded(cashbackAwarded);
        attemptRepository.save(attempt);

        Long contestId = contestService.findContestIdByAttemptId(attemptId);
        boolean contestAttempt = contestId != null;
        List<ReviewQuestion> reviewQuestions = questions.stream().map(question -> {
            String selectedOption = selectedByQuestion.get(question.getId());
            boolean isCorrect = selectedOption != null && question.getCorrectOption() != null
                    && question.getCorrectOption().equalsIgnoreCase(selectedOption);
            return new ReviewQuestion(question.getId(), question.getQuestionText(),
                    question.getOptionA(), question.getOptionB(), question.getOptionC(), question.getOptionD(),
                    contestAttempt ? null : question.getCorrectOption(), selectedOption, contestAttempt ? false : isCorrect);
        }).toList();
        return new SubmitAttemptResponse(attemptId, contestId, contestAttempt ? contestService.answerKeyAvailableAt(contestId) : null, correctCount, attempt.getTotalQuestions(), cashbackAwarded, attempt.getCoinsSpent(), reviewQuestions);
    }

    public List<ReviewQuestion> getContestAnswerKey(Long contestId) {
        com.aptigen.contest.WeeklyContest contest = contestService.requireReleasedAnswerKey(contestId);
        return questionRepository.findByTestId(contest.getTestId()).stream()
                .map(question -> new ReviewQuestion(question.getId(), question.getQuestionText(),
                        question.getOptionA(), question.getOptionB(), question.getOptionC(), question.getOptionD(),
                        question.getCorrectOption(), null, false))
                .toList();
    }
}
