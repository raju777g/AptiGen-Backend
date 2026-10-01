package com.aptigen.generation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_folders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestFolder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;
    @Column(name = "parent_id")
    private Long parentId;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}