package com.aptigen.user;

import com.aptigen.common.AuthUtil;
import com.aptigen.storage.FileStorageService;
import com.aptigen.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users/me/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final UserRepository userRepository;
    private final UserService userService;

    private final PasswordResetService passwordResetService;
    private final AuthUtil authUtil;
    private final FileStorageService fileStorageService;

    private static final Set<String> PREDEFINED_KEYS = Set.of(
            "avatar1", "avatar2", "avatar3", "avatar4", "avatar5", "avatar6"
    );
    private static final long MAX_SIZE_BYTES = 3 * 1024 * 1024; // 3MB — plenty for a profile pic

    // Option A: pick one of the predefined avatars (served from the frontend's own /src/assets)
    record PredefinedAvatarRequest(String avatarKey) {}

       record ResetRequestRequest(String email) {}
    record ResetVerifyRequest(String email, String otp, String newPassword) {}

    @PostMapping("/predefined")
    public ResponseEntity<?> setPredefined(Authentication auth, @RequestBody PredefinedAvatarRequest request) {
        if (!PREDEFINED_KEYS.contains(request.avatarKey())) {
            throw new IllegalArgumentException("Unknown avatar option");
        }
        User user = userService.findByEmail(authUtil.extractEmail(auth));
        user.setAvatarUrl("predefined:" + request.avatarKey()); // frontend resolves this prefix to its own asset
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("avatarUrl", user.getAvatarUrl()));
    }

    // Option B: upload a custom image
    @PostMapping("/upload")
    public ResponseEntity<?> upload(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (file.getSize() > MAX_SIZE_BYTES) throw new IllegalArgumentException("Image must be under 3MB");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        User user = userService.findByEmail(authUtil.extractEmail(auth));

        StoredFile storedFile = fileStorageService.store(file);
        String url = "/api/files/" + storedFile.getId();
        user.setAvatarUrl(url);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ResetRequestRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset code has been sent."));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<?> verifyPasswordReset(@RequestBody ResetVerifyRequest request) {
        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        passwordResetService.verifyAndReset(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}
