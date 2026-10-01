package com.aptigen.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqService {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneratedMcqBatch parseIntoMcqs(String rawOcrText, int requestedQuestionCount) {
        try {
            return parseSingleBatch(rawOcrText, requestedQuestionCount);
        } catch (RuntimeException failure) {
            if (requestedQuestionCount <= 10 || !isJsonGenerationFailure(failure)) throw failure;

            List<GeneratedMcq> questions = new ArrayList<>();
            String sourceType = null;
            for (int start = 0; start < requestedQuestionCount; start += 8) {
                int batchSize = Math.min(8, requestedQuestionCount - start);
                StringBuilder batchInput = new StringBuilder(rawOcrText);
                batchInput.append("\n\nBATCH INSTRUCTION: This is batch ")
                        .append(start / 8 + 1)
                        .append(". ");
                if (start == 0) {
                    batchInput.append("Return the first ").append(batchSize).append(" questions.");
                } else {
                    batchInput.append("Skip the first ").append(start)
                            .append(" questions in document order and return the next ")
                            .append(batchSize).append(" questions.");
                }
                if (!questions.isEmpty()) {
                    batchInput.append(" For generated NOTES questions, do not repeat these existing question stems: ")
                            .append(questions.stream().map(GeneratedMcq::questionText).toList());
                }

                GeneratedMcqBatch batch = parseSingleBatch(batchInput.toString(), batchSize);
                if (batch.questions() == null || batch.questions().isEmpty()) break;
                sourceType = sourceType == null ? batch.sourceType() : sourceType;
                questions.addAll(batch.questions());
                if ("MCQS".equalsIgnoreCase(batch.sourceType()) && batch.questions().size() < batchSize) break;
            }
            if (questions.isEmpty()) throw failure;
            return new GeneratedMcqBatch(
                    questions.subList(0, Math.min(requestedQuestionCount, questions.size())), sourceType);
        }
    }

    private boolean isJsonGenerationFailure(Throwable failure) {
        String message = failure.getMessage();
        return message != null && message.contains("400 Bad Request")
                && message.contains("json_validate_failed");
    }

    private GeneratedMcqBatch parseSingleBatch(String rawOcrText, int requestedQuestionCount) {
        String systemPrompt = """
               You process messy OCR text from an English printed page and return ONLY valid JSON.

               Rules:
               - The text may contain "--- Page N ---" markers separating multiple pages of the same document. Treat them as one continuous document — extract every question across all pages, and ignore the markers themselves in your output.
               - If an answer key or marked answer is present, use it. If no answer is visible, solve each readable MCQ independently using your subject knowledge.
               - OCR is imperfect: fix obvious typos in option text using context (e.g. "py Answer" for a Python file
                 extension question should become option text ".py"). Use your judgement, don't invent new questions.
               - The user requested exactly %d questions.
               - If the OCR text contains existing MCQs, extract only the first %d readable MCQs in document order. Do not generate replacement questions when fewer than %d MCQs are available; return only the available questions.
               - If the OCR text contains study notes or explanatory printed text instead of existing MCQs, generate exactly %d distinct MCQs from facts explicitly present in that text.
               - Keep each question and option concise so all requested questions fit in one JSON response.
               - Set sourceType to MCQS for existing questions and NOTES for generated questions. Always include sourceType.
               - "topic" should be a short category guess, e.g. "Programming", "Quantitative", "Logical", "Verbal".
               - correctOption must be exactly one of "A", "B", "C", "D".
               - Return JSON matching exactly this shape, nothing else, no markdown fences:
               {"sourceType":"MCQS or NOTES","questions":[{"questionText":"...","optionA":"...","optionB":"...","optionC":"...","optionD":"...","correctOption":"A","topic":"..."}]}
               """.formatted(requestedQuestionCount, requestedQuestionCount, requestedQuestionCount, requestedQuestionCount);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_completion_tokens", 32768,
                "reasoning_effort", "low",
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", rawOcrText)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    "https://api.groq.com/openai/v1/chat/completions", entity, Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Groq returned an empty response");
            }
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            }

            GeneratedMcqBatch batch = objectMapper.readValue(content, GeneratedMcqBatch.class);
            if (batch.questions() == null || batch.questions().isEmpty()) {
                throw new IllegalStateException("Groq returned no readable questions from OCR text");
            }
            if (batch.questions().size() > requestedQuestionCount) {
                return new GeneratedMcqBatch(
                        batch.questions().subList(0, requestedQuestionCount), batch.sourceType());
            }
            return batch;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse MCQs from AI response: " + e.getMessage(), e);
        }
    }

    public GeneratedMcqBatch generateFromNotes(String rawText, int questionCount) {
        String systemPrompt = """
           You are creating a multiple-choice practice quiz FROM the study notes/paragraph text given to you.
           This text does NOT already contain questions — you must invent %d original MCQs that test
           understanding of the key facts, concepts, and ideas in the text.

           Rules:
           - Generate exactly %d questions if the content supports it; fewer only if the text is too short
             to responsibly support that many distinct, non-repetitive questions.
           - Each question must have 4 plausible options (A-D), exactly one correct, based on the text.
           - Don't invent facts not supported by the text.
           - "topic" should be a short category guess based on the subject matter.
           - correctOption must be exactly one of "A", "B", "C", "D".
           - Return JSON matching exactly this shape, nothing else, no markdown fences:
           {"questions":[{"questionText":"...","optionA":"...","optionB":"...","optionC":"...","optionD":"...","correctOption":"A","topic":"..."}]}
           """.formatted(questionCount, questionCount);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_completion_tokens", 32768,
                "reasoning_effort", "low",
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", rawText)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    "https://api.groq.com/openai/v1/chat/completions", entity, Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            return objectMapper.readValue(content, GeneratedMcqBatch.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate questions from notes: " + e.getMessage(), e);
        }
    }
}
