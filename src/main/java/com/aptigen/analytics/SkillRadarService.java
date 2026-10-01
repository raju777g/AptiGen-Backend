package com.aptigen.analytics;

import com.aptigen.attempt.AttemptAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillRadarService {

    private final AttemptAnswerRepository attemptAnswerRepository;

    public record TopicAccuracy(String topic, int accuracyPercent, long totalAnswered, java.time.LocalDateTime lastAttemptAt) {}

    public List<TopicAccuracy> getSkillRadar(Long userId) {
        return attemptAnswerRepository.getTopicAccuracy(userId).stream()
                .filter(row -> row.getTopic() != null)
                .map(row -> {
                    long total = row.getTotalCount();
                    long correct = row.getCorrectCount();
                    int percent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
                    return new TopicAccuracy(row.getTopic(), percent, total, row.getLastAttemptAt());
                })
                .sorted(java.util.Comparator.comparing(TopicAccuracy::lastAttemptAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public List<TopicAccuracy> getPlatformAverage() {
        return attemptAnswerRepository.getPlatformTopicAccuracy().stream()
                .filter(row -> row.getTopic() != null)
                .map(row -> {
                    long total = row.getTotalCount();
                    long correct = row.getCorrectCount();
                    int percent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
                    return new TopicAccuracy(row.getTopic(), percent, total, row.getLastAttemptAt());
                })
                .sorted(java.util.Comparator.comparing(TopicAccuracy::lastAttemptAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }
}