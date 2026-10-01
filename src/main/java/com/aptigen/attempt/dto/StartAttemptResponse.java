package com.aptigen.attempt.dto;

import java.util.List;
public record StartAttemptResponse(
        Long attemptId, List<QuestionForAttempt> questions, int secondsPerQuestion
) {}