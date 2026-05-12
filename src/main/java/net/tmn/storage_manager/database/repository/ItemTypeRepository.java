package net.tmn.storage_manager.database.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tmn.storage_manager.database.jpa.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemTypeRepository extends JpaRepository<ItemType, UUID> {
    Optional<ItemType> findByName(String name);

    @Query("SELECT it FROM ItemType it ORDER BY it.name")
    List<ItemType> findAllOrderByName();

    boolean existsByName(String name);
}
