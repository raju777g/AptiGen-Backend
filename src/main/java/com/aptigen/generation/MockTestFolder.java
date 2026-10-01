package com.aptigen.generation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mock_test_folders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MockTestFolder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "parent_id")
    private Long parentId;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
