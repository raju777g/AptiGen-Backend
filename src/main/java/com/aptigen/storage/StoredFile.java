package com.aptigen.storage;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stored_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
