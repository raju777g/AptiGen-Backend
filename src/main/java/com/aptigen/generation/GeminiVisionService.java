package com.aptigen.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

@Service
public class GeminiVisionService {

    @Value("${gemini.vision.api-key:}")
    private String apiKey;

    @Value("${gemini.vision.api-keys:}")
    private String apiKeys;

    @Value("${gemini.vision.model}")
    private String model;

    @Value("${gemini.vision.fallback-model}")
    private String fallbackModel;

    @Value("${gemini.vision.tertiary-model}")
    private String tertiaryModel;

    @Value("${gemini.vision.quaternary-model}")
    private String quaternaryModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public GeneratedMcqBatch generateQuestionsFromImages(List<MultipartFile> images,
                                                          GenerationMode mode,
                                                          Integer desiredQuestionCount) throws IOException {
        if (configuredApiKeys().isEmpty()) {
            throw new IllegalStateException("Gemini vision is not configured. Set GEMINI_VISION_API_KEY.");
        }

        String prompt = buildPrompt(mode, desiredQuestionCount);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        for (MultipartFile image : images) {
            String contentType = image.getContentType();
            String mimeType = contentType != null && contentType.startsWith("image/") ? contentType : "image/jpeg";
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", mimeType,
                    "data", Base64.getEncoder().encodeToString(image.getBytes())
            )));
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 8192, "responseMimeType", "application/json")
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = requestAnyAvailableModel(request);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini could not extract questions from the uploaded images.");
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> responseParts = content == null ? null : (List<Map<String, Object>>) content.get("parts");
        StringBuilder json = new StringBuilder();
        if (responseParts != null) {
            for (Map<String, Object> part : responseParts) {
                Object text = part.get("text");
                if (text instanceof String value) json.append(value);
            }
        }
        if (json.toString().isBlank()) throw new IllegalStateException("Gemini returned no question data.");
        GeneratedMcqBatch batch = objectMapper.readValue(json.toString(), GeneratedMcqBatch.class);
        if (batch.questions() == null || batch.questions().isEmpty()) {
            throw new IllegalStateException("Gemini did not find any usable questions in these images.");
        }
        return batch;
    }

    private Map<String, Object> requestAnyAvailableModel(HttpEntity<Map<String, Object>> request) {
        List<String> keys = configuredApiKeys();
        Throwable lastFailure = null;
        for (String candidateKey : keys) {
            try {
                // Exactly one request per key. The next key is not contacted
                // unless this request fails or returns unusable content.
                return requestWithKey(candidateKey, request);
            } catch (RuntimeException failure) {
                lastFailure = failure;
            }
        }
        throw new IllegalStateException("All configured Gemini API keys failed. Please retry shortly.", lastFailure);
    }

    private Map<String, Object> requestWithKey(String candidateKey,
                                               HttpEntity<Map<String, Object>> request) {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("GEMINI_VISION_MODEL is not configured.");
        }
        Map<String, Object> response = requestModel(model, candidateKey, request);
        if (!hasUsableTextResponse(response)) {
            throw new IllegalStateException("Gemini returned no usable content.");
        }
        return response;
    }

    private Map<String, Object> requestModel(String selectedModel, String candidateKey,
                                              HttpEntity<Map<String, Object>> request) {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + selectedModel + ":generateContent";
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.set("x-goog-api-key", candidateKey);
        Map<String, Object> response = restTemplate.postForObject(
                endpoint, new HttpEntity<>(request.getBody(), headers), Map.class);
        if (response == null) throw new IllegalStateException("Gemini returned an empty response.");
        return response;
    }

    private boolean hasUsableTextResponse(Map<String, Object> response) {
        Object candidates = response == null ? null : response.get("candidates");
        if (!(candidates instanceof List<?> candidateList) || candidateList.isEmpty()) return false;
        Object first = candidateList.get(0);
        if (!(first instanceof Map<?, ?> firstCandidate)) return false;
        Object content = firstCandidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) return false;
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partList)) return false;
        return partList.stream().anyMatch(part -> part instanceof Map<?, ?> map
                && map.get("text") instanceof String text && !text.isBlank());
    }

    private List<String> configuredApiKeys() {
        Set<String> keys = new LinkedHashSet<>();
        if (apiKeys != null && !apiKeys.isBlank()) {
            Arrays.stream(apiKeys.split(","))
                    .map(String::trim)
                    .filter(key -> !key.isBlank())
                    .forEach(keys::add);
        }
        if (apiKey != null && !apiKey.isBlank()) keys.add(apiKey.trim());
        return new ArrayList<>(keys);
    }

    private boolean isTemporaryFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private String buildPrompt(GenerationMode mode, Integer desiredQuestionCount) {
        String outputFormat = "Return only JSON in this exact shape, with no markdown: "
                + "{\"sourceType\":\"MCQS or NOTES\",\"questions\":[{\"questionText\":\"...\",\"optionA\":\"...\",\"optionB\":\"...\",\"optionC\":\"...\",\"optionD\":\"...\",\"correctOption\":\"A\",\"topic\":\"...\"}]}";
        String sharedRules = "Treat all uploaded images as pages of one document. Read the question numbering and all four options faithfully. "
                + "Correct obvious visual/OCR-like recognition mistakes only when context makes the intended text clear; never invent or silently rewrite source content. "
                + "Keep each question, option, and topic in its original language and script. Never translate or transliterate; preserve Bengali in Bengali script and Hindi in Devanagari. "
                + "For existing MCQs, use an answer key or marked answer when present. If no answer is shown, solve each readable MCQ independently using subject knowledge and select the correct option. "
                + "Do not skip a readable MCQ merely because the image has no answer key. Skip only questions/options that are too unclear to recover. "
                + "Every output question must have four plausible options and exactly one correct answer. Use a short topic label in the question's language. correctOption must be exactly A, B, C, or D.";

        if (mode == GenerationMode.FROM_NOTES) {
            int count = desiredQuestionCount == null ? 10 : desiredQuestionCount;
            return "The images are study notes, not an existing MCQ test. Create exactly " + count + " distinct multiple-choice questions from their key facts and concepts. "
                    + "Do not add facts unsupported by the notes. Set sourceType to NOTES. " + sharedRules + " " + outputFormat;
        }
        String mcqInstructions = "The images contain existing multiple-choice questions. Extract every readable question and its four options without generating replacements. "
                + "Set sourceType to MCQS. " + sharedRules + " " + outputFormat;
        if (mode == GenerationMode.FROM_MCQS) return mcqInstructions;

        int count = desiredQuestionCount == null ? 10 : desiredQuestionCount;
        return "Inspect the images and identify their content type. If they contain one or more existing MCQs, extract those questions and set sourceType to MCQS; do not generate new questions in this case. "
                + "If they contain notes/explanatory material and no existing MCQ questions, create exactly " + count + " distinct MCQs grounded in the key ideas and set sourceType to NOTES. "
                + "When generating from notes, do not add unsupported facts. " + sharedRules + " " + outputFormat;
    }
}
