package com.aptigen.attempt.dto;

public record ReviewQuestion(
        Long questionId,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String selectedOption,
        boolean isCorrect
) {}