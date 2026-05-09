package net.tmn.storage_manager.service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.database.repository.ProduceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProduceTypeService {

    ProduceTypeRepository produceTypeRepository;

    @Transactional(readOnly = true)
    public List<ProduceType> getAllProduceTypes() {
        return produceTypeRepository.findAllOrderByName();
    }

    @Transactional(readOnly = true)
    public Optional<ProduceType> getProduceTypeById(UUID id) {
        return produceTypeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ProduceType> getProduceTypeByName(String name) {
        return produceTypeRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public ProduceTypeTransferData exportProduceTypes() {
        List<ProduceTypeTransferData.ProduceTypeRecord> produceTypes =
                produceTypeRepository.findAllOrderByName().stream()
                        .map(this::toTransferRecord)
                        .toList();

        return new ProduceTypeTransferData(ProduceTypeTransferData.CURRENT_SCHEMA_VERSION, Instant.now(), produceTypes);
    }

    @Transactional
    public int importProduceTypes(ProduceTypeTransferData transferData) {
        if (transferData == null) {
            throw new IllegalArgumentException("Import file is empty");
        }

        Set<String> importedNames = new LinkedHashSet<>();
        for (ProduceTypeTransferData.ProduceTypeRecord record : transferData.produceTypes()) {
            validateTransferRecord(record, importedNames);
        }

        int importedCount = 0;
        for (ProduceTypeTransferData.ProduceTypeRecord record : transferData.produceTypes()) {
            String produceTypeName = record.name().trim();

            ProduceType produceType =
                    produceTypeRepository.findByName(produceTypeName).orElseGet(ProduceType::new);

            applyTransferRecord(produceType, record, produceTypeName);
            produceTypeRepository.save(produceType);
            importedCount++;
        }

        return importedCount;
    }

    @Transactional
    public ProduceType createProduceType(ProduceType produceType, MultipartFile image) {
        if (produceTypeRepository.existsByName(produceType.getName())) {
            throw new IllegalArgumentException("Produce type with name '" + produceType.getName() + "' already exists");
        }

        if (image != null && !image.isEmpty()) {
            try {
                byte[] imageBytes = image.getBytes();
                log.info("Saving image with {} bytes", imageBytes.length);
                produceType.setImageData(imageBytes);
                produceType.setImageContentType(image.getContentType());
                produceType.setImageFilename(image.getOriginalFilename());
            } catch (IOException e) {
                log.error("Failed to save image for produce type: {}", produceType.getName(), e);
                throw new RuntimeException("Failed to save image", e);
            }
        }

        return produceTypeRepository.save(produceType);
    }

    @Transactional
    public ProduceType updateProduceType(UUID id, ProduceType updatedProduceType, MultipartFile image) {
        ProduceType existingProduceType = produceTypeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce type not found with id: " + id));

        // Check if name is being changed and if it conflicts with existing names
        if (!existingProduceType.getName().equals(updatedProduceType.getName())
                && produceTypeRepository.existsByName(updatedProduceType.getName())) {
            throw new IllegalArgumentException(
                    "Produce type with name '" + updatedProduceType.getName() + "' already exists");
        }

        existingProduceType.setName(updatedProduceType.getName());
        existingProduceType.setDescription(updatedProduceType.getDescription());
        existingProduceType.setPrice(updatedProduceType.getPrice());
        existingProduceType.setNotificationDaysModifier(updatedProduceType.getNotificationDaysModifier());

        if (image != null && !image.isEmpty()) {
            try {
                byte[] imageBytes = image.getBytes();
                log.info("Saving image with {} bytes", imageBytes.length);
                existingProduceType.setImageData(imageBytes);
                existingProduceType.setImageContentType(image.getContentType());
                existingProduceType.setImageFilename(image.getOriginalFilename());
            } catch (IOException e) {
                log.error("Failed to save image for produce type: {}", existingProduceType.getName(), e);
                throw new RuntimeException("Failed to save image", e);
            }
        }

        return produceTypeRepository.save(existingProduceType);
    }

    @Transactional
    public void deleteProduceType(UUID id) {
        ProduceType produceType = produceTypeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce type not found with id: " + id));

        produceTypeRepository.delete(produceType);
    }

    private ProduceTypeTransferData.ProduceTypeRecord toTransferRecord(ProduceType produceType) {
        return new ProduceTypeTransferData.ProduceTypeRecord(
                produceType.getName(),
                produceType.getDescription(),
                produceType.getPrice(),
                produceType.getNotificationDaysModifier(),
                produceType.getImageFilename(),
                produceType.getImageContentType(),
                produceType.getImageData());
    }

    private void applyTransferRecord(
            ProduceType produceType, ProduceTypeTransferData.ProduceTypeRecord record, String produceTypeName) {
        produceType.setName(produceTypeName);
        produceType.setDescription(record.description());
        produceType.setPrice(record.price());
        produceType.setNotificationDaysModifier(record.notificationDaysModifier());
        produceType.setImageFilename(record.imageFilename());
        produceType.setImageContentType(record.imageContentType());
        produceType.setImageData(record.imageData());
    }

    private void validateTransferRecord(ProduceTypeTransferData.ProduceTypeRecord record, Set<String> importedNames) {
        if (record == null || !StringUtils.hasText(record.name())) {
            throw new IllegalArgumentException("Each imported produce type must include a name");
        }

        String normalizedName = record.name().trim();
        if (!importedNames.add(normalizedName)) {
            throw new IllegalArgumentException("Import file contains duplicate produce type name: " + normalizedName);
        }
    }
}
