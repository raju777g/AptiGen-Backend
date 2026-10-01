package com.aptigen.attempt;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "coins_spent", nullable = false)
    @Builder.Default
    private int coinsSpent = 0;

    @Column(name = "cashback_awarded", nullable = false)
    @Builder.Default
    private boolean cashbackAwarded = false;
}