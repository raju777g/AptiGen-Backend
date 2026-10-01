package com.aptigen.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StoredFileRepository repository;

    public StoredFile store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Unknown file type");
        }
        return repository.save(StoredFile.builder()
                .fileData(file.getBytes())
                .contentType(contentType)
                .originalName(file.getOriginalFilename())
                .build());
    }

    public StoredFile get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }
}
