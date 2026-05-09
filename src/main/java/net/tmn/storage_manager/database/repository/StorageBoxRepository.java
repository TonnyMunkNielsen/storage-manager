package net.tmn.storage_manager.database.repository;

import java.util.List;
import java.util.UUID;
import net.tmn.storage_manager.database.jpa.StorageBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageBoxRepository extends JpaRepository<StorageBox, UUID> {
    @Query("SELECT sb FROM StorageBox sb ORDER BY sb.boxNumber")
    List<StorageBox> findAllOrderByName();
}
