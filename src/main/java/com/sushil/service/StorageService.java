package com.sushil.service;

import com.sushil.exception.AppExceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long        MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB

    @Value("${app.storage.upload-dir:uploads/profiles}")
    private String uploadDir;

    @Value("${app.storage.base-url:http://localhost:8080/uploads/profiles}")
    private String baseUrl;

    @Async
    public CompletableFuture<String> storeProfilePicAsync(MultipartFile file) {
        return CompletableFuture.completedFuture(storeProfilePic(file));
    }

    /**
     * Saves the profile picture and returns its public URL.
     */
    public String storeProfilePic(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        validateFile(file);

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            String ext      = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + ext;
            Path   target   = dir.resolve(filename);
            file.transferTo(target);

            String url = baseUrl + "/" + filename;
            log.info("[STORAGE] Profile pic stored at '{}' ({} bytes)", target, file.getSize());
            return url;
        } catch (IOException ex) {
            log.error("[STORAGE] Failed to store profile pic: {}", ex.getMessage());
            throw new RuntimeException("File storage failed", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Profile picture must be under 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, or WebP images are allowed");
        }
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return "jpg";
    }
}
