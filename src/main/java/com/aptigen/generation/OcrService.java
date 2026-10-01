package com.aptigen.generation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OcrService {

    @Value("${ocr.space.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public String extractText(byte[] imageBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("apikey", apiKey);
        body.add("language", "eng");
        body.add("isOverlayRequired", "false");
        body.add("detectOrientation", "true");
        body.add("scale", "true");
        body.add("OCREngine", "2");
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? "upload.jpg" : filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(
                "https://api.ocr.space/parse/image", requestEntity, Map.class);

        if (response == null || Boolean.TRUE.equals(response.get("IsErroredOnProcessing"))) {
            throw new RuntimeException("OCR failed: " + response);
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("ParsedResults");
        if (results == null || results.isEmpty()) return "";

        StringBuilder parsedText = new StringBuilder();
        for (Map<String, Object> result : results) {
            if (result == null) continue;
            Object pageText = result.get("ParsedText");
            if (pageText instanceof String text && !text.isBlank()) {
                if (parsedText.length() > 0) parsedText.append('\n');
                parsedText.append(text);
            }
        }
        return parsedText.toString();
    }
}
