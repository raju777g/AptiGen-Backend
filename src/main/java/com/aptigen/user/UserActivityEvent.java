package com.aptigen.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserActivityEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
