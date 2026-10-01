package com.aptigen.generation;

import com.aptigen.wallet.TransactionType;
import com.aptigen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestGenerationService {

    private final GeminiVisionService geminiVisionService;
    private final OcrService ocrService;
    private final GroqService groqService;
    private final McqTestRepository testRepository;
    private final McqQuestionRepository questionRepository;
    private final WalletService walletService;
    private final com.aptigen.user.UserService userService;

    private static final int MAX_PAGES = 10;

    @Value("${aptigen.notes-generation-coin-cost-per-question}")
    private int notesGenCoinCostPerQuestion;

    @Transactional
    public McqTest generateTest(Long userId, List<MultipartFile> images, String title,
                                TimerMode timerMode, int secondsPerQuestion,
                                GenerationMode mode, Integer desiredQuestionCount) throws IOException {

        if (images == null || images.isEmpty()) throw new IllegalArgumentException("At least one image is required");
        if (images.size() > MAX_PAGES) throw new IllegalArgumentException("Maximum " + MAX_PAGES + " pages per upload");
        if ((mode == GenerationMode.FROM_NOTES || mode == GenerationMode.AUTO || mode == GenerationMode.ENGLISH_PRINTED)
                && (desiredQuestionCount == null || desiredQuestionCount < 1 || desiredQuestionCount > 30)) {
            throw new IllegalArgumentException("Question count must be between 1 and 30");
        }

        GeneratedMcqBatch batch = extractBatch(images, mode, desiredQuestionCount);

        if (mode == GenerationMode.AUTO && (batch.sourceType() == null
                || (!batch.sourceType().equalsIgnoreCase("MCQS") && !batch.sourceType().equalsIgnoreCase("NOTES")))) {
            throw new IllegalStateException("Gemini could not determine whether the upload contains notes or existing MCQs. Please retry with a clearer image.");
        }

        if (batch.questions() == null || batch.questions().isEmpty()) {
            String hint = mode == GenerationMode.FROM_NOTES
                    ? "Try uploading text with more substantial content."
                    : "Make sure the image contains readable notes or multiple-choice questions.";
            throw new IllegalStateException("No questions could be generated. " + hint);
        }

        boolean generatedFromNotes = "NOTES".equalsIgnoreCase(batch.sourceType())
                || (batch.sourceType() == null && mode == GenerationMode.FROM_NOTES);
        int coinPerQuestion = generatedFromNotes ? notesGenCoinCostPerQuestion : 1;
        int coinCost = batch.questions().size() * coinPerQuestion;
        McqTest test = testRepository.save(McqTest.builder()
                .ownerUserId(userId)
                .title(title)
                .timerMode(timerMode)
                .secondsPerQuestion(secondsPerQuestion)
                .build());

        walletService.debit(userId, coinCost, TransactionType.MCQ_GENERATION, test.getId());
        batch.questions().forEach(q -> questionRepository.save(McqQuestion.builder()
                .testId(test.getId())
                .questionText(q.questionText())
                .optionA(q.optionA())
                .optionB(q.optionB())
                .optionC(q.optionC())
                .optionD(q.optionD())
                .correctOption(q.correctOption())
                .topic(q.topic())
                .build()));
        return test;
    }

    private GeneratedMcqBatch extractBatch(List<MultipartFile> images, GenerationMode mode,
                                            Integer desiredQuestionCount) throws IOException {
        if (mode == GenerationMode.HANDWRITTEN_OR_OTHER_LANGUAGE) {
            // Gemini receives the original images so it can understand handwriting and non-English scripts.
            return geminiVisionService.generateQuestionsFromImages(images, GenerationMode.AUTO, desiredQuestionCount);
        }
        if (mode == GenerationMode.ENGLISH_PRINTED) {
            StringBuilder extractedText = new StringBuilder();
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                String text = ocrService.extractText(image.getBytes(), image.getOriginalFilename());
                if (text != null && !text.isBlank()) {
                    extractedText.append("--- Page ").append(i + 1).append(" ---\n").append(text).append('\n');
                }
            }
            if (extractedText.isEmpty()) {
                throw new IllegalStateException("OCR could not read any text from the uploaded images.");
            }
            int questionCount = desiredQuestionCount == null ? 10 : desiredQuestionCount;
            return groqService.parseIntoMcqs(extractedText.toString(), questionCount);
        }
        return geminiVisionService.generateQuestionsFromImages(images, mode, desiredQuestionCount);
    }

    @Transactional
    public McqTest generateTestForEmail(String email, List<MultipartFile> images, String title,
                                        TimerMode timerMode, int secondsPerQuestion,
                                        GenerationMode mode, Integer desiredQuestionCount) throws IOException {
        Long userId = userService.findByEmail(email).getId();
        return generateTest(userId, images, title, timerMode, secondsPerQuestion, mode, desiredQuestionCount);
    }
}
