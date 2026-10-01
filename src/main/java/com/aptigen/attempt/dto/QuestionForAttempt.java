package com.aptigen.attempt.dto;

import java.util.List;

public record QuestionForAttempt(
        Long questionId, String questionText,
        String optionA, String optionB, String optionC, String optionD
) {}