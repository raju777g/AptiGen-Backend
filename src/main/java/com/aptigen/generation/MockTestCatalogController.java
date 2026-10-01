package com.aptigen.generation;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MockTestCatalogController {
    private final McqTestRepository testRepository;
    private final McqQuestionRepository questionRepository;
    private final MockTestFolderRepository folderRepository;
    private final UserService userService;
    private final AuthUtil authUtil;

    record FolderSummary(Long id, String name, Long parentId) {}
    record TestSummary(Long id, String title, long questionCount, int secondsPerQuestion,
                       java.time.LocalDateTime createdAt, Long folderId) {}
    record CatalogResponse(List<FolderSummary> folders, List<TestSummary> tests) {}
    record FolderRequest(String name, Long parentId) {}
    record PublishRequest(Long folderId) {}

    @GetMapping("/api/mock-tests")
    public ResponseEntity<?> catalog() {
        List<FolderSummary> folders = folderRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(folder -> new FolderSummary(folder.getId(), folder.getName(), folder.getParentId())).toList();
        List<TestSummary> tests = testRepository.findByIsMockTestTrue().stream()
                .map(this::toSummary).toList();
        return ResponseEntity.ok(new CatalogResponse(folders, tests));
    }

    @PostMapping("/api/admin/mock-folders")
    public ResponseEntity<?> createFolder(Authentication auth, @RequestBody FolderRequest request) {
        requireAdmin(auth);
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 80) return ResponseEntity.badRequest().body("Folder name must be 1-80 characters");
        if (request.parentId() != null) folderRepository.findById(request.parentId()).orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        MockTestFolder folder = folderRepository.save(MockTestFolder.builder().name(name).parentId(request.parentId()).build());
        return ResponseEntity.ok(new FolderSummary(folder.getId(), folder.getName(), folder.getParentId()));
    }

    @PatchMapping("/api/admin/mock-folders/{id}")
    public ResponseEntity<?> renameFolder(Authentication auth, @PathVariable Long id, @RequestBody FolderRequest request) {
        requireAdmin(auth);
        MockTestFolder folder = folderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 80) return ResponseEntity.badRequest().body("Folder name must be 1-80 characters");
        if (request.parentId() != null) {
            MockTestFolder parent = folderRepository.findById(request.parentId()).orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
            if (parent.getId().equals(id)) return ResponseEntity.badRequest().body("A folder cannot be moved inside itself");
        }
        folder.setName(name);
        folder.setParentId(request.parentId());
        folder = folderRepository.save(folder);
        return ResponseEntity.ok(new FolderSummary(folder.getId(), folder.getName(), folder.getParentId()));
    }

    @DeleteMapping("/api/admin/mock-folders/{id}")
    public ResponseEntity<?> deleteFolder(Authentication auth, @PathVariable Long id) {
        requireAdmin(auth);
        folderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/mock-tests/{id}/publish")
    public ResponseEntity<?> publish(Authentication auth, @PathVariable Long id, @RequestBody PublishRequest request) {
        Long adminId = requireAdmin(auth);
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        if (!test.getOwnerUserId().equals(adminId)) return ResponseEntity.status(403).body("Only tests created by this administrator can be added");
        if (request.folderId() != null) folderRepository.findById(request.folderId()).orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        test.setPublic(true);
        test.setMockTest(true);
        test.setMockFolderId(request.folderId());
        return ResponseEntity.ok(toSummary(testRepository.save(test)));
    }

    @DeleteMapping("/api/admin/mock-tests/{id}/publish")
    public ResponseEntity<?> unpublish(Authentication auth, @PathVariable Long id) {
        Long adminId = requireAdmin(auth);
        McqTest test = testRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        if (!test.getOwnerUserId().equals(adminId)) return ResponseEntity.status(403).body("Only the test owner can remove it");
        test.setMockTest(false);
        test.setMockFolderId(null);
        return ResponseEntity.ok(toSummary(testRepository.save(test)));
    }

    private TestSummary toSummary(McqTest test) {
        return new TestSummary(test.getId(), test.getTitle(), questionRepository.countByTestId(test.getId()),
                test.getSecondsPerQuestion(), test.getCreatedAt(), test.getMockFolderId());
    }

    private Long requireAdmin(Authentication auth) {
        var user = userService.findByEmail(authUtil.extractEmail(auth));
        if (!"ADMIN".equals(user.getRole())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Administrator access required");
        return user.getId();
    }
}
