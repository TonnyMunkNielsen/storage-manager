package net.tmn.storage_manager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.ItemInstanceStatus;
import net.tmn.storage_manager.database.repository.ItemInstanceRepository;
import net.tmn.storage_manager.database.repository.ItemTypeRepository;
import net.tmn.storage_manager.database.repository.StorageBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemInstanceService {

    ItemInstanceRepository itemInstanceRepository;
    ItemTypeRepository itemTypeRepository;
    StorageBoxRepository storageBoxRepository;

    @Transactional(readOnly = true)
    public List<ItemInstance> getAllItemInstances() {
        return itemInstanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ItemInstance> getActiveItemInstances() {
        return itemInstanceRepository.findAllActiveInstances();
    }

    @Transactional(readOnly = true)
    public Optional<ItemInstance> getItemInstanceById(UUID id) {
        return itemInstanceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ItemInstance> getItemInstancesByItemType(UUID itemTypeId) {
        return itemInstanceRepository.findByItemTypeIdOrderByBestBeforeDate(itemTypeId);
    }

    @Transactional(readOnly = true)
    public List<ItemInstance> getActiveItemInstancesByItemType(UUID itemTypeId) {
        return itemInstanceRepository.findActiveInstancesByItemType(itemTypeId);
    }

    @Transactional(readOnly = true)
    public List<ItemInstance> getExpiredItemInstances() {
        return itemInstanceRepository.findInstancesExpiredBefore(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ItemInstance> getItemInstancesExpiringBetween(LocalDate startDate, LocalDate endDate) {
        return itemInstanceRepository.findActiveInstancesExpiringBetween(startDate, endDate);
    }

    @Transactional
    public ItemInstance createItemInstance(ItemInstance itemInstance) {
        itemInstance.setItemType(resolveItemType(itemInstance));
        itemInstance.setStorageBox(resolveStorageBox(itemInstance));
        applyExpiredStatusIfPastBestBefore(itemInstance);

        return itemInstanceRepository.save(itemInstance);
    }

    @Transactional
    public ItemInstance updateItemInstance(UUID id, ItemInstance updatedItemInstance) {
        ItemInstance existingInstance = itemInstanceRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item instance not found with id: " + id));

        existingInstance.setTitle(updatedItemInstance.getTitle());
        existingInstance.setBestBeforeDate(updatedItemInstance.getBestBeforeDate());
        existingInstance.setStatus(updatedItemInstance.getStatus());
        existingInstance.setStorageBox(resolveStorageBox(updatedItemInstance));
        applyExpiredStatusIfPastBestBefore(existingInstance);

        return itemInstanceRepository.save(existingInstance);
    }

    @Transactional
    public ItemInstance replaceItemInstance(UUID id, ItemInstance newInstance) {
        ItemInstance existingInstance = itemInstanceRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item instance not found with id: " + id));

        // Create new instance with same item type
        newInstance.setItemType(existingInstance.getItemType());
        newInstance.setStorageBox(resolveReplacementStorageBox(existingInstance, newInstance));
        ItemInstance savedNewInstance = itemInstanceRepository.save(newInstance);

        // Mark existing instance as replaced
        existingInstance.setStatus(ItemInstanceStatus.REPLACED);
        existingInstance.setReplacedBy(savedNewInstance);
        existingInstance.setReplacedAt(LocalDateTime.now());
        itemInstanceRepository.save(existingInstance);

        return savedNewInstance;
    }

    @Transactional
    public void deleteItemInstance(UUID id) {
        ItemInstance itemInstance = itemInstanceRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item instance not found with id: " + id));

        itemInstanceRepository.delete(itemInstance);
    }

    @Transactional
    public void updateExpiredInstances() {
        List<ItemInstance> expiredInstances = itemInstanceRepository.findActiveInstancesExpiredBefore(LocalDate.now());
        expiredInstances.forEach(instance -> {
            instance.setStatus(ItemInstanceStatus.EXPIRED);
            itemInstanceRepository.save(instance);
        });

        if (!expiredInstances.isEmpty()) {
            log.info("Updated {} expired item instances", expiredInstances.size());
        }
    }

    private void applyExpiredStatusIfPastBestBefore(ItemInstance itemInstance) {
        if (itemInstance.getStatus() == ItemInstanceStatus.ACTIVE
                && itemInstance.getBestBeforeDate() != null
                && itemInstance.getBestBeforeDate().isBefore(LocalDate.now())) {
            itemInstance.setStatus(ItemInstanceStatus.EXPIRED);
        }
    }

    private ItemType resolveItemType(ItemInstance itemInstance) {
        if (itemInstance.getItemType() == null || itemInstance.getItemType().getId() == null) {
            throw new IllegalArgumentException("Item type is required");
        }

        return itemTypeRepository
                .findById(itemInstance.getItemType().getId())
                .orElseThrow(() -> new IllegalArgumentException("Item type not found with id: "
                        + itemInstance.getItemType().getId()));
    }

    private StorageBox resolveStorageBox(ItemInstance itemInstance) {
        if (itemInstance.getStorageBox() == null || itemInstance.getStorageBox().getId() == null) {
            throw new IllegalArgumentException("Storage box is required");
        }

        return storageBoxRepository
                .findById(itemInstance.getStorageBox().getId())
                .orElseThrow(() -> new IllegalArgumentException("Storage box not found with id: "
                        + itemInstance.getStorageBox().getId()));
    }

    private StorageBox resolveReplacementStorageBox(ItemInstance existingInstance, ItemInstance newInstance) {
        if (newInstance.getStorageBox() == null || newInstance.getStorageBox().getId() == null) {
            return existingInstance.getStorageBox();
        }

        return resolveStorageBox(newInstance);
    }
}
