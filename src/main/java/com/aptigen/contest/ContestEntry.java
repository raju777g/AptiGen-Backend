package com.aptigen.contest;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contest_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "contest_rank")
    private Integer rank;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}