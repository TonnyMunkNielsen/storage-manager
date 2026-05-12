package net.tmn.storage_manager.database.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.database.jpa.type.ItemInstanceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemInstanceRepository extends JpaRepository<ItemInstance, UUID> {
    @Override
    @EntityGraph(attributePaths = {"itemType", "storageBox"})
    List<ItemInstance> findAll();

    @Override
    @EntityGraph(attributePaths = {"itemType", "storageBox"})
    java.util.Optional<ItemInstance> findById(UUID id);

    @EntityGraph(attributePaths = {"itemType", "storageBox"})
    List<ItemInstance> findByItemTypeIdOrderByBestBeforeDate(UUID itemTypeId);

    @EntityGraph(attributePaths = {"itemType", "storageBox"})
    List<ItemInstance> findByStatusOrderByBestBeforeDate(ItemInstanceStatus status);

    default List<ItemInstance> findInstancesExpiredBefore(LocalDate date) {
        return findInstancesExpiredBefore(date, List.of(ItemInstanceStatus.ACTIVE, ItemInstanceStatus.EXPIRED));
    }

    default List<ItemInstance> findActiveInstancesExpiredBefore(LocalDate date) {
        return findInstancesExpiredBefore(date, List.of(ItemInstanceStatus.ACTIVE));
    }

    @Query(
            "SELECT ii FROM ItemInstance ii JOIN FETCH ii.itemType JOIN FETCH ii.storageBox WHERE ii.status IN :statuses AND ii.bestBeforeDate < :date ORDER BY ii.bestBeforeDate")
    List<ItemInstance> findInstancesExpiredBefore(
            @Param("date") LocalDate date, @Param("statuses") Collection<ItemInstanceStatus> statuses);

    @Query(
            "SELECT ii FROM ItemInstance ii JOIN FETCH ii.itemType JOIN FETCH ii.storageBox WHERE ii.status = 'ACTIVE' AND ii.bestBeforeDate BETWEEN :startDate AND :endDate ORDER BY ii.bestBeforeDate")
    List<ItemInstance> findActiveInstancesExpiringBetween(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(
            "SELECT ii FROM ItemInstance ii JOIN FETCH ii.itemType JOIN FETCH ii.storageBox WHERE ii.itemType.id = :itemTypeId AND ii.status = 'ACTIVE' ORDER BY ii.bestBeforeDate")
    List<ItemInstance> findActiveInstancesByItemType(@Param("itemTypeId") UUID itemTypeId);

    @Query(
            "SELECT ii FROM ItemInstance ii JOIN FETCH ii.itemType JOIN FETCH ii.storageBox WHERE ii.status = 'ACTIVE' ORDER BY ii.bestBeforeDate")
    List<ItemInstance> findAllActiveInstances();

    long countByItemTypeIdAndStatus(UUID itemTypeId, ItemInstanceStatus status);
}
