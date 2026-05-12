package net.tmn.storage_manager.service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.repository.ItemTypeRepository;
import net.tmn.storage_manager.service.validation.DomainValidator;
import net.tmn.storage_manager.service.validation.ImageValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemTypeService {

    ItemTypeRepository itemTypeRepository;
    ImageValidator imageValidator;
    DomainValidator domainValidator;

    @Transactional(readOnly = true)
    public List<ItemType> getAllItemTypes() {
        return itemTypeRepository.findAllOrderByName();
    }

    @Transactional(readOnly = true)
    public Optional<ItemType> getItemTypeById(UUID id) {
        return itemTypeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ItemType> getItemTypeByName(String name) {
        return itemTypeRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public ItemTypeTransferData exportItemTypes() {
        List<ItemTypeTransferData.ItemTypeRecord> itemTypes = itemTypeRepository.findAllOrderByName().stream()
                .map(this::toTransferRecord)
                .toList();

        return new ItemTypeTransferData(ItemTypeTransferData.CURRENT_SCHEMA_VERSION, Instant.now(), itemTypes);
    }

    @Transactional
    public int importItemTypes(ItemTypeTransferData transferData) {
        if (transferData == null) {
            throw new IllegalArgumentException("Import file is empty");
        }
        if (transferData.schemaVersion() != ItemTypeTransferData.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported import schema version: " + transferData.schemaVersion());
        }

        Set<String> importedNames = new LinkedHashSet<>();
        for (ItemTypeTransferData.ItemTypeRecord record : transferData.itemTypes()) {
            validateTransferRecord(record, importedNames);
        }

        int importedCount = 0;
        for (ItemTypeTransferData.ItemTypeRecord record : transferData.itemTypes()) {
            String itemTypeName = normalizeName(record.name());

            ItemType itemType = itemTypeRepository.findByName(itemTypeName).orElseGet(ItemType::new);

            applyTransferRecord(itemType, record, itemTypeName);
            saveValidated(itemType);
            importedCount++;
        }

        return importedCount;
    }

    @Transactional
    public ItemType createItemType(ItemType itemType, MultipartFile image) {
        return createItemType(itemType, toUploadedImage(image));
    }

    @Transactional
    public ItemType createItemType(ItemType itemType, UploadedImage image) {
        prepareItemTypeForSave(itemType);
        if (itemTypeRepository.existsByName(itemType.getName())) {
            throw new IllegalArgumentException("Item type with name '" + itemType.getName() + "' already exists");
        }

        applyImage(itemType, image);

        return saveValidated(itemType);
    }

    @Transactional
    public ItemType updateItemType(UUID id, ItemType updatedItemType, MultipartFile image) {
        return updateItemType(id, updatedItemType, toUploadedImage(image));
    }

    @Transactional
    public ItemType updateItemType(UUID id, ItemType updatedItemType, UploadedImage image) {
        prepareItemTypeForSave(updatedItemType);
        ItemType existingItemType = itemTypeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item type not found with id: " + id));

        // Check if name is being changed and if it conflicts with existing names
        if (!Objects.equals(existingItemType.getName(), updatedItemType.getName())
                && itemTypeRepository.existsByName(updatedItemType.getName())) {
            throw new IllegalArgumentException(
                    "Item type with name '" + updatedItemType.getName() + "' already exists");
        }

        existingItemType.setName(updatedItemType.getName());
        existingItemType.setDescription(updatedItemType.getDescription());
        existingItemType.setPrice(updatedItemType.getPrice());
        existingItemType.setNotificationDaysModifier(updatedItemType.getNotificationDaysModifier());

        applyImage(existingItemType, image);

        return saveValidated(existingItemType);
    }

    @Transactional
    public void deleteItemType(UUID id) {
        ItemType itemType = itemTypeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item type not found with id: " + id));

        itemTypeRepository.delete(itemType);
    }

    private ItemTypeTransferData.ItemTypeRecord toTransferRecord(ItemType itemType) {
        return new ItemTypeTransferData.ItemTypeRecord(
                itemType.getName(),
                itemType.getDescription(),
                itemType.getPrice(),
                itemType.getNotificationDaysModifier(),
                itemType.getImageFilename(),
                itemType.getImageContentType(),
                itemType.getImageData());
    }

    private void applyTransferRecord(
            ItemType itemType, ItemTypeTransferData.ItemTypeRecord record, String itemTypeName) {
        itemType.setName(itemTypeName);
        itemType.setDescription(record.description());
        itemType.setPrice(record.price());
        itemType.setNotificationDaysModifier(record.notificationDaysModifier());
        itemType.setImageFilename(record.imageFilename());
        itemType.setImageContentType(record.imageContentType());
        itemType.setImageData(record.imageData());
    }

    private void validateTransferRecord(ItemTypeTransferData.ItemTypeRecord record, Set<String> importedNames) {
        if (record == null || !StringUtils.hasText(record.name())) {
            throw new IllegalArgumentException("Each imported item type must include a name");
        }

        String normalizedName = normalizeName(record.name());
        if (!importedNames.add(normalizedName)) {
            throw new IllegalArgumentException("Import file contains duplicate item type name: " + normalizedName);
        }

        ItemType itemType = new ItemType();
        applyTransferRecord(itemType, record, normalizedName);
        domainValidator.validate(itemType);
        imageValidator.validateImage(toUploadedImage(record));
    }

    private void prepareItemTypeForSave(ItemType itemType) {
        if (itemType == null) {
            throw new IllegalArgumentException("Item type is required");
        }

        itemType.setName(normalizeName(itemType.getName()));
        domainValidator.validate(itemType);
    }

    private ItemType saveValidated(ItemType itemType) {
        domainValidator.validate(itemType);
        return itemTypeRepository.save(itemType);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private UploadedImage toUploadedImage(ItemTypeTransferData.ItemTypeRecord record) {
        return new UploadedImage(record.imageFilename(), record.imageContentType(), record.imageData());
    }

    private UploadedImage toUploadedImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        imageValidator.validateImage(image);
        try {
            return new UploadedImage(image.getOriginalFilename(), image.getContentType(), image.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image", e);
        }
    }

    private void applyImage(ItemType itemType, UploadedImage image) {
        imageValidator.validateImage(image);

        if (image == null || image.isEmpty()) {
            return;
        }

        log.info("Saving image with {} bytes", image.size());
        itemType.setImageData(image.bytes());
        itemType.setImageContentType(image.contentType());
        itemType.setImageFilename(image.filename());
    }
}
