package net.tmn.storage_manager.database.repository;

import net.tmn.produce.database.jpa.StorageBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StorageBoxRepository extends JpaRepository<StorageBox, UUID> {
    @Query("SELECT sb FROM StorageBox sb ORDER BY sb.boxNumber")
    List<StorageBox> findAllOrderByName();
}
