package net.tmn.storage_manager.web.rest;

import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.database.repository.ProduceTypeRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImageController {

    ProduceTypeRepository produceTypeRepository;

    @GetMapping("/produce-type/{id}")
    public ResponseEntity<byte[]> getProduceTypeImage(@PathVariable UUID id) {
        Optional<ProduceType> produceType = produceTypeRepository.findById(id);

        // Return 404 if produce type doesn't exist OR if it has no image data
        if (produceType.isEmpty()
                || produceType.get().getImageData() == null
                || produceType.get().getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        ProduceType type = produceType.get();

        HttpHeaders headers = new HttpHeaders();

        // Use a default content type if none is specified
        String contentType = type.getImageContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(type.getImageData().length);

        // Optional: Set cache headers
        headers.setCacheControl(CacheControl.noCache().mustRevalidate());

        return ResponseEntity.ok().headers(headers).body(type.getImageData());
    }
}
