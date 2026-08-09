package com.gpsvariant.controller;

import com.gpsvariant.entity.GpsImage;
import com.gpsvariant.entity.User;
import com.gpsvariant.repository.GpsImageRepository;
import com.gpsvariant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/image")
public class ImageUploadController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final GpsImageRepository repository;
    private final UserRepository userRepository;
    private final Path uploadDirectory;
    private final long maxUploadBytes;

    public ImageUploadController(
            GpsImageRepository repository,
            UserRepository userRepository,
            @Value("${app.upload-dir:uploads}") String uploadDir,
            @Value("${app.max-upload-bytes:10485760}") long maxUploadBytes) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please select an image"));
        }

        if (file.getSize() > maxUploadBytes) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("message", "Image exceeds the maximum allowed size"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only JPG, PNG and WebP images are allowed"));
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }

        String username = authentication.getName().trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));

        Files.createDirectories(uploadDirectory);

        String extension = extensionFor(contentType);
        String fileName = UUID.randomUUID() + extension;
        Path filePath = uploadDirectory.resolve(fileName).normalize();

        if (!filePath.startsWith(uploadDirectory)) {
            throw new SecurityException("Invalid upload path");
        }

        try {
            Files.copy(file.getInputStream(), filePath);

            GpsImage image = new GpsImage();
            image.setImagePath(filePath.toString());
            image.setUser(user);
            image = repository.save(image);

            return ResponseEntity.ok(Map.of(
                    "message", "Uploaded Successfully",
                    "imageId", image.getId(),
                    "imagePath", "/uploads/" + fileName
            ));
        } catch (Exception ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
