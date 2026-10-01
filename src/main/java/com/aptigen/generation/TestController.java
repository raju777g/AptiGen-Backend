package com.aptigen.generation;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {
    private final McqTestRepository testRepository;
    private final McqQuestionRepository questionRepository;
    private final TestFolderRepository folderRepository;
    private final UserService userService;
    private final AuthUtil authUtil;

    record ExportQuestion(String questionText, String optionA, String optionB, String optionC, String optionD) {}
    record TestExport(String title, java.time.LocalDateTime createdAt, List<ExportQuestion> questions) {}

    @GetMapping("/{id}/export-data")
    public ResponseEntity<?> exportData(Authentication auth, @PathVariable Long id) {
        Long userId = currentUserId(auth);
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        if (!test.isPublic() && !test.getOwnerUserId().equals(userId)) {
            throw new IllegalStateException("This test is private");
        }
        List<ExportQuestion> questions = questionRepository.findByTestId(id).stream()
                .map(q -> new ExportQuestion(q.getQuestionText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                .toList();
        return ResponseEntity.ok(new TestExport(test.getTitle(), test.getCreatedAt(), questions));
    }
    record TestSummary(Long id, String title, boolean isPublic, TimerMode timerMode,
                       int secondsPerQuestion, long questionCount, java.time.LocalDateTime createdAt,
                       Long folderId, boolean isMockTest, Long mockFolderId) {}
    record FolderSummary(Long id, String name, Long parentId, java.time.LocalDateTime createdAt) {}
    record CreateFolderRequest(String name, Long parentId) {}
    record MoveTestRequest(Long folderId) {}
    record RenameTestRequest(String title) {}
    record CopyTestRequest(Long folderId) {}
    record CopyFolderRequest(Long parentId) {}
    record RenameFolderRequest(String name) {}
    record MoveFolderRequest(Long parentId) {}

    private Long currentUserId(Authentication auth) {
        return userService.findByEmail(authUtil.extractEmail(auth)).getId();
    }

    private TestSummary toSummary(McqTest t) {
        long count = questionRepository.countByTestId(t.getId());
        return new TestSummary(t.getId(), t.getTitle(), t.isPublic(), t.getTimerMode(),
                t.getSecondsPerQuestion(), count, t.getCreatedAt(), t.getFolderId(), t.isMockTest(), t.getMockFolderId());
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myTests(Authentication auth) {
        List<TestSummary> tests = testRepository.findByOwnerUserId(currentUserId(auth))
                .stream().map(this::toSummary).collect(Collectors.toList());
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/folders")
    public ResponseEntity<?> myFolders(Authentication auth) {
        return ResponseEntity.ok(folderRepository.findByOwnerUserIdOrderByCreatedAtAsc(currentUserId(auth))
                .stream().map(f -> new FolderSummary(f.getId(), f.getName(), f.getParentId(), f.getCreatedAt())).toList());
    }

    @PostMapping("/folders")
    public ResponseEntity<?> createFolder(Authentication auth, @RequestBody CreateFolderRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 80) return ResponseEntity.badRequest().body("Folder name must be 1Ã¢â‚¬â€œ80 characters");
        Long userId = currentUserId(auth);
        if (request.parentId() != null) requireOwnedFolder(request.parentId(), userId);
        TestFolder folder = folderRepository.save(TestFolder.builder().ownerUserId(userId).parentId(request.parentId()).name(name).build());
        return ResponseEntity.ok(new FolderSummary(folder.getId(), folder.getName(), folder.getParentId(), folder.getCreatedAt()));
    }

    @PatchMapping("/folders/{id}/rename")
    public ResponseEntity<?> renameFolder(Authentication auth, @PathVariable Long id, @RequestBody RenameFolderRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 80) return ResponseEntity.badRequest().body("Folder name must be 1-80 characters");
        TestFolder folder = requireOwnedFolder(id, currentUserId(auth));
        folder.setName(name);
        return ResponseEntity.ok(toFolderSummary(folderRepository.save(folder)));
    }

    @PatchMapping("/folders/{id}/move")
    public ResponseEntity<?> moveFolder(Authentication auth, @PathVariable Long id, @RequestBody MoveFolderRequest request) {
        Long userId = currentUserId(auth);
        TestFolder folder = requireOwnedFolder(id, userId);
        if (request.parentId() != null) {
            TestFolder parent = requireOwnedFolder(request.parentId(), userId);
            for (TestFolder ancestor = parent; ancestor != null; ancestor = ancestor.getParentId() == null ? null : requireOwnedFolder(ancestor.getParentId(), userId)) {
                if (ancestor.getId().equals(id)) return ResponseEntity.badRequest().body("A folder cannot be moved inside itself");
            }
        }
        folder.setParentId(request.parentId());
        return ResponseEntity.ok(toFolderSummary(folderRepository.save(folder)));
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<?> deleteFolder(Authentication auth, @PathVariable Long id) {
        TestFolder folder = requireOwnedFolder(id, currentUserId(auth));
        folderRepository.delete(folder);
        return ResponseEntity.noContent().build();
    }

    private FolderSummary toFolderSummary(TestFolder folder) {
        return new FolderSummary(folder.getId(), folder.getName(), folder.getParentId(), folder.getCreatedAt());
    }

    @PatchMapping("/{id}/folder")
    public ResponseEntity<?> moveTest(Authentication auth, @PathVariable Long id, @RequestBody MoveTestRequest request) {
        Long userId = currentUserId(auth);
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        if (!test.getOwnerUserId().equals(userId)) throw new IllegalStateException("You don't own this test");
        if (request.folderId() != null) requireOwnedFolder(request.folderId(), userId);
        test.setFolderId(request.folderId());
        testRepository.save(test);
        return ResponseEntity.ok(toSummary(test));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<?> renameTest(Authentication auth, @PathVariable Long id, @RequestBody RenameTestRequest request) {
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        Long userId = currentUserId(auth);
        if (!test.getOwnerUserId().equals(userId)) throw new IllegalStateException("You don't own this test");
        String title = request.title() == null ? "" : request.title().trim();
        if (title.isEmpty() || title.length() > 160) return ResponseEntity.badRequest().body("Test name must be 1-160 characters");
        test.setTitle(title);
        return ResponseEntity.ok(toSummary(testRepository.save(test)));
    }

    @PostMapping("/{id}/copy")
    @Transactional
    public ResponseEntity<?> copyTest(Authentication auth, @PathVariable Long id, @RequestBody(required = false) CopyTestRequest request) {
        McqTest source = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        Long userId = currentUserId(auth);
        if (!source.getOwnerUserId().equals(userId)) throw new IllegalStateException("You don't own this test");

        Long folderId = request == null ? source.getFolderId() : request.folderId();
        if (folderId != null) requireOwnedFolder(folderId, userId);
        McqTest copy = testRepository.save(McqTest.builder()
                .ownerUserId(userId)
                .title(source.getTitle())
                .isPublic(false)
                .timerMode(source.getTimerMode())
                .secondsPerQuestion(source.getSecondsPerQuestion())
                .folderId(folderId)
                .isMockTest(false)
                .mockFolderId(null)
                .build());
        List<McqQuestion> questions = questionRepository.findByTestId(id).stream()
                .map(question -> McqQuestion.builder()
                        .testId(copy.getId())
                        .questionText(question.getQuestionText())
                        .optionA(question.getOptionA())
                        .optionB(question.getOptionB())
                        .optionC(question.getOptionC())
                        .optionD(question.getOptionD())
                        .correctOption(question.getCorrectOption())
                        .topic(question.getTopic())
                        .build())
                .toList();
        questionRepository.saveAll(questions);
        return ResponseEntity.ok(toSummary(copy));
    }

    @PostMapping("/folders/{id}/copy")
    @Transactional
    public ResponseEntity<?> copyFolder(Authentication auth, @PathVariable Long id, @RequestBody(required = false) CopyFolderRequest request) {
        Long userId = currentUserId(auth);
        TestFolder source = requireOwnedFolder(id, userId);
        Long parentId = request == null ? source.getParentId() : request.parentId();
        if (parentId != null) {
            TestFolder parent = requireOwnedFolder(parentId, userId);
            for (TestFolder ancestor = parent; ancestor != null; ancestor = ancestor.getParentId() == null ? null : requireOwnedFolder(ancestor.getParentId(), userId)) {
                if (ancestor.getId().equals(source.getId())) return ResponseEntity.badRequest().body("A folder cannot be copied inside itself");
            }
        }
        TestFolder copy = copyFolderTree(source, userId, parentId);
        return ResponseEntity.ok(toFolderSummary(copy));
    }

    private TestFolder copyFolderTree(TestFolder source, Long userId, Long parentId) {
        TestFolder copy = folderRepository.save(TestFolder.builder()
                .ownerUserId(userId)
                .parentId(parentId)
                .name(source.getName())
                .build());
        testRepository.findByOwnerUserId(userId).stream()
                .filter(test -> source.getId().equals(test.getFolderId()))
                .forEach(test -> copyTestIntoFolder(test, userId, copy.getId()));
        folderRepository.findByOwnerUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(folder -> source.getId().equals(folder.getParentId()))
                .forEach(child -> copyFolderTree(child, userId, copy.getId()));
        return copy;
    }

    private void copyTestIntoFolder(McqTest source, Long userId, Long folderId) {
        McqTest copy = testRepository.save(McqTest.builder()
                .ownerUserId(userId)
                .title(source.getTitle())
                .isPublic(false)
                .timerMode(source.getTimerMode())
                .secondsPerQuestion(source.getSecondsPerQuestion())
                .folderId(folderId)
                .isMockTest(false)
                .mockFolderId(null)
                .build());
        questionRepository.saveAll(questionRepository.findByTestId(source.getId()).stream()
                .map(question -> McqQuestion.builder()
                        .testId(copy.getId())
                        .questionText(question.getQuestionText())
                        .optionA(question.getOptionA())
                        .optionB(question.getOptionB())
                        .optionC(question.getOptionC())
                        .optionD(question.getOptionD())
                        .correctOption(question.getCorrectOption())
                        .topic(question.getTopic())
                        .build())
                .toList());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteTest(Authentication auth, @PathVariable Long id) {
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        Long userId = currentUserId(auth);
        if (!test.getOwnerUserId().equals(userId)) throw new IllegalStateException("You don't own this test");
        questionRepository.deleteAll(questionRepository.findByTestId(id));
        testRepository.delete(test);
        return ResponseEntity.noContent().build();
    }

    private TestFolder requireOwnedFolder(Long folderId, Long userId) {
        TestFolder folder = folderRepository.findById(folderId).orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        if (!folder.getOwnerUserId().equals(userId)) throw new IllegalStateException("You don't own this folder");
        return folder;
    }

    @GetMapping("/public")
    public ResponseEntity<?> publicTests() {
        return ResponseEntity.ok(testRepository.findByIsPublicTrue().stream().map(this::toSummary).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(Authentication auth, @PathVariable Long id) {
        Long userId = currentUserId(auth);
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        if (!test.getOwnerUserId().equals(userId)) throw new IllegalStateException("You don't own this test");
        test.setPublic(true);
        testRepository.save(test);
        return ResponseEntity.ok(toSummary(test));
    }
}
