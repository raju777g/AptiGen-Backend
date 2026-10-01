package com.aptigen.generation;

import com.aptigen.common.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class GenerationController {

    private final TestGenerationService testGenerationService;
    private final AuthUtil authUtil;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> generate(
            Authentication authentication,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("title") String title,
            @RequestParam(value = "timerMode", defaultValue = "STANDARD") TimerMode timerMode,
            @RequestParam(value = "secondsPerQuestion", defaultValue = "120") int secondsPerQuestion,
            @RequestParam(value = "mode", defaultValue = "HANDWRITTEN_OR_OTHER_LANGUAGE") GenerationMode mode,
            @RequestParam(value = "questionCount", required = false) Integer questionCount
    ) throws IOException {
        String email = authUtil.extractEmail(authentication);
        McqTest test = testGenerationService.generateTestForEmail(
                email, images, title, timerMode, secondsPerQuestion, mode, questionCount);
        return ResponseEntity.ok(test);
    }

}
