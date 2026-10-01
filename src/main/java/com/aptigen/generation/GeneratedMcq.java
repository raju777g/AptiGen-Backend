package com.aptigen.generation;

public record GeneratedMcq(
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption, // "A", "B", "C", or "D"
        String topic          // e.g. "Programming", "Quantitative", "Logical", "Verbal"
) {}