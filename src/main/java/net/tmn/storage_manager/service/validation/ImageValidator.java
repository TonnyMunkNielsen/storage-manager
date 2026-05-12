package net.tmn.storage_manager.service.validation;

import java.util.Set;
import net.tmn.storage_manager.service.UploadedImage;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    public void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image size cannot exceed 5MB");
        }

        if (!ALLOWED_TYPES.contains(image.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF and WebP images are allowed");
        }
    }

    public void validateImage(UploadedImage image) {
        if (image == null || image.isEmpty()) {
            return;
        }

        if (image.size() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image size cannot exceed 5MB");
        }

        if (!ALLOWED_TYPES.contains(image.contentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF and WebP images are allowed");
        }
    }
}
