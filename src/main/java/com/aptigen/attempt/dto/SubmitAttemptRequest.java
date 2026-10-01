package com.aptigen.attempt.dto;

import java.util.List;

public record SubmitAttemptRequest(List<AnswerSubmission> answers) {}