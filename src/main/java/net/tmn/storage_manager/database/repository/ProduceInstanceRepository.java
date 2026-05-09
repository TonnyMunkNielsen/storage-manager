package net.tmn.storage_manager.database.repository;

import net.tmn.produce.database.jpa.ProduceInstance;
import net.tmn.produce.database.jpa.type.ProduceInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProduceInstanceRepository extends JpaRepository<ProduceInstance, UUID> {
    List<ProduceInstance> findByProduceTypeIdOrderByBestBeforeDate(UUID produceTypeId);

    List<ProduceInstance> findByStatusOrderByBestBeforeDate(ProduceInstanceStatus status);

    @Query("SELECT pi FROM ProduceInstance pi WHERE pi.status = 'EXPIRED' AND pi.bestBeforeDate <= :date ORDER BY pi.bestBeforeDate")
    List<ProduceInstance> findInstancesExpiredBefore(@Param("date") LocalDate date);

    @Query("SELECT pi FROM ProduceInstance pi WHERE pi.status = 'ACTIVE' AND pi.bestBeforeDate BETWEEN :startDate AND :endDate ORDER BY pi.bestBeforeDate")
    List<ProduceInstance> findActiveInstancesExpiringBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT pi FROM ProduceInstance pi WHERE pi.produceType.id = :produceTypeId AND pi.status = 'ACTIVE' ORDER BY pi.bestBeforeDate")
    List<ProduceInstance> findActiveInstancesByProduceType(@Param("produceTypeId") UUID produceTypeId);

    @Query("SELECT pi FROM ProduceInstance pi WHERE pi.status = 'ACTIVE' ORDER BY pi.bestBeforeDate")
    List<ProduceInstance> findAllActiveInstances();

    long countByProduceTypeIdAndStatus(UUID produceTypeId, ProduceInstanceStatus status);
}