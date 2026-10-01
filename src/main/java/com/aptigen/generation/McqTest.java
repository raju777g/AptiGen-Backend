package com.aptigen.generation;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mcq_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McqTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private String title;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "timer_mode", nullable = false)
    @Builder.Default
    private TimerMode timerMode = TimerMode.STANDARD;

    @Column(name = "seconds_per_question", nullable = false)
    @Builder.Default
    private int secondsPerQuestion = 120;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "is_mock_test", nullable = false)
    @Builder.Default
    private boolean isMockTest = false;

    @Column(name = "mock_folder_id")
    private Long mockFolderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
