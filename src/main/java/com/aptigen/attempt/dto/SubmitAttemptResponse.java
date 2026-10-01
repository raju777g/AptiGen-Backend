package com.aptigen.attempt.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubmitAttemptResponse(
        Long attemptId, Long contestId, LocalDateTime answerKeyAvailableAt,
        int score, int totalQuestions, boolean cashbackAwarded, int coinsSpent,
        List<ReviewQuestion> reviewQuestions
) {}