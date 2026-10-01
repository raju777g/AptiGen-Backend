package com.aptigen.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        StoredFile file = fileStorageService.get(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.getContentType());
        } catch (InvalidMediaTypeException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.getFileData().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.getOriginalName() == null ? "file" : file.getOriginalName())
                        .build().toString())
                .body(new ByteArrayResource(file.getFileData()));
    }
}
