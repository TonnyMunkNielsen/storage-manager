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

        Set<String> importedNames = new LinkedHashSet<>();
        for (ItemTypeTransferData.ItemTypeRecord record : transferData.itemTypes()) {
            validateTransferRecord(record, importedNames);
        }

        int importedCount = 0;
        for (ItemTypeTransferData.ItemTypeRecord record : transferData.itemTypes()) {
            String itemTypeName = record.name().trim();

            ItemType itemType = itemTypeRepository.findByName(itemTypeName).orElseGet(ItemType::new);

            applyTransferRecord(itemType, record, itemTypeName);
            itemTypeRepository.save(itemType);
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
        if (itemTypeRepository.existsByName(itemType.getName())) {
            throw new IllegalArgumentException("Item type with name '" + itemType.getName() + "' already exists");
        }

        applyImage(itemType, image);

        return itemTypeRepository.save(itemType);
    }

    @Transactional
    public ItemType updateItemType(UUID id, ItemType updatedItemType, MultipartFile image) {
        return updateItemType(id, updatedItemType, toUploadedImage(image));
    }

    @Transactional
    public ItemType updateItemType(UUID id, ItemType updatedItemType, UploadedImage image) {
        ItemType existingItemType = itemTypeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item type not found with id: " + id));

        // Check if name is being changed and if it conflicts with existing names
        if (!existingItemType.getName().equals(updatedItemType.getName())
                && itemTypeRepository.existsByName(updatedItemType.getName())) {
            throw new IllegalArgumentException(
                    "Item type with name '" + updatedItemType.getName() + "' already exists");
        }

        existingItemType.setName(updatedItemType.getName());
        existingItemType.setDescription(updatedItemType.getDescription());
        existingItemType.setPrice(updatedItemType.getPrice());
        existingItemType.setNotificationDaysModifier(updatedItemType.getNotificationDaysModifier());

        applyImage(existingItemType, image);

        return itemTypeRepository.save(existingItemType);
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

        String normalizedName = record.name().trim();
        if (!importedNames.add(normalizedName)) {
            throw new IllegalArgumentException("Import file contains duplicate item type name: " + normalizedName);
        }
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
