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
import net.tmn.storage_manager.database.jpa.ProduceInstance;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.ProduceInstanceStatus;
import net.tmn.storage_manager.database.repository.ProduceInstanceRepository;
import net.tmn.storage_manager.database.repository.ProduceTypeRepository;
import net.tmn.storage_manager.database.repository.StorageBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProduceInstanceService {

    ProduceInstanceRepository produceInstanceRepository;
    ProduceTypeRepository produceTypeRepository;
    StorageBoxRepository storageBoxRepository;

    @Transactional(readOnly = true)
    public List<ProduceInstance> getAllProduceInstances() {
        return produceInstanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProduceInstance> getActiveProduceInstances() {
        return produceInstanceRepository.findAllActiveInstances();
    }

    @Transactional(readOnly = true)
    public Optional<ProduceInstance> getProduceInstanceById(UUID id) {
        return produceInstanceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ProduceInstance> getProduceInstancesByProduceType(UUID produceTypeId) {
        return produceInstanceRepository.findByProduceTypeIdOrderByBestBeforeDate(produceTypeId);
    }

    @Transactional(readOnly = true)
    public List<ProduceInstance> getActiveProduceInstancesByProduceType(UUID produceTypeId) {
        return produceInstanceRepository.findActiveInstancesByProduceType(produceTypeId);
    }

    @Transactional(readOnly = true)
    public List<ProduceInstance> getExpiredProduceInstances() {
        return produceInstanceRepository.findInstancesExpiredBefore(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ProduceInstance> getProduceInstancesExpiringBetween(LocalDate startDate, LocalDate endDate) {
        return produceInstanceRepository.findActiveInstancesExpiringBetween(startDate, endDate);
    }

    @Transactional
    public ProduceInstance createProduceInstance(ProduceInstance produceInstance) {
        produceInstance.setProduceType(resolveProduceType(produceInstance));
        produceInstance.setStorageBox(resolveStorageBox(produceInstance));

        return produceInstanceRepository.save(produceInstance);
    }

    @Transactional
    public ProduceInstance updateProduceInstance(UUID id, ProduceInstance updatedProduceInstance) {
        ProduceInstance existingInstance = produceInstanceRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce instance not found with id: " + id));

        existingInstance.setTitle(updatedProduceInstance.getTitle());
        existingInstance.setBestBeforeDate(updatedProduceInstance.getBestBeforeDate());
        existingInstance.setStatus(updatedProduceInstance.getStatus());
        existingInstance.setStorageBox(resolveStorageBox(updatedProduceInstance));

        return produceInstanceRepository.save(existingInstance);
    }

    @Transactional
    public ProduceInstance replaceProduceInstance(UUID id, ProduceInstance newInstance) {
        ProduceInstance existingInstance = produceInstanceRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce instance not found with id: " + id));

        // Create new instance with same produce type
        newInstance.setProduceType(existingInstance.getProduceType());
        newInstance.setStorageBox(resolveReplacementStorageBox(existingInstance, newInstance));
        ProduceInstance savedNewInstance = produceInstanceRepository.save(newInstance);

        // Mark existing instance as replaced
        existingInstance.setStatus(ProduceInstanceStatus.REPLACED);
        existingInstance.setReplacedBy(savedNewInstance);
        existingInstance.setReplacedAt(LocalDateTime.now());
        produceInstanceRepository.save(existingInstance);

        return savedNewInstance;
    }

    @Transactional
    public void deleteProduceInstance(UUID id) {
        ProduceInstance produceInstance = produceInstanceRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce instance not found with id: " + id));

        produceInstanceRepository.delete(produceInstance);
    }

    @Transactional
    public void updateExpiredInstances() {
        List<ProduceInstance> expiredInstances = produceInstanceRepository.findInstancesExpiredBefore(LocalDate.now());
        expiredInstances.forEach(instance -> {
            instance.setStatus(ProduceInstanceStatus.EXPIRED);
            produceInstanceRepository.save(instance);
        });

        if (!expiredInstances.isEmpty()) {
            log.info("Updated {} expired produce instances", expiredInstances.size());
        }
    }

    private ProduceType resolveProduceType(ProduceInstance produceInstance) {
        if (produceInstance.getProduceType() == null
                || produceInstance.getProduceType().getId() == null) {
            throw new IllegalArgumentException("Produce type is required");
        }

        return produceTypeRepository
                .findById(produceInstance.getProduceType().getId())
                .orElseThrow(() -> new IllegalArgumentException("Produce type not found with id: "
                        + produceInstance.getProduceType().getId()));
    }

    private StorageBox resolveStorageBox(ProduceInstance produceInstance) {
        if (produceInstance.getStorageBox() == null
                || produceInstance.getStorageBox().getId() == null) {
            throw new IllegalArgumentException("Storage box is required");
        }

        return storageBoxRepository
                .findById(produceInstance.getStorageBox().getId())
                .orElseThrow(() -> new IllegalArgumentException("Storage box not found with id: "
                        + produceInstance.getStorageBox().getId()));
    }

    private StorageBox resolveReplacementStorageBox(ProduceInstance existingInstance, ProduceInstance newInstance) {
        if (newInstance.getStorageBox() == null || newInstance.getStorageBox().getId() == null) {
            return existingInstance.getStorageBox();
        }

        return resolveStorageBox(newInstance);
    }
}
