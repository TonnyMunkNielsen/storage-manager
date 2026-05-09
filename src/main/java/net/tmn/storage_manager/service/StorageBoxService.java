package net.tmn.storage_manager.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.repository.StorageBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorageBoxService {

    StorageBoxRepository storageBoxRepository;

    @Transactional(readOnly = true)
    public List<StorageBox> getAllStorageBoxes() {
        return storageBoxRepository.findAllOrderByName();
    }

    @Transactional(readOnly = true)
    public Optional<StorageBox> getStorageBoxById(UUID id) {
        return storageBoxRepository.findById(id);
    }

    @Transactional
    public void createStorageBox(StorageBox box) {
        storageBoxRepository.save(box);
    }

    @Transactional
    public void updateStorageBox(UUID id, StorageBox updatedBox) {
        StorageBox existing = storageBoxRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage box not found"));

        existing.setDessicantChangedDate(updatedBox.getDessicantChangedDate());
        existing.setStatus(updatedBox.getStatus());

        storageBoxRepository.save(existing);
    }

    @Transactional
    public void deleteStorageBox(UUID id) {
        storageBoxRepository.deleteById(id);
    }
}
