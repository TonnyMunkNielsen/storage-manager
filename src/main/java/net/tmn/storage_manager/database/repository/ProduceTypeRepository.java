package net.tmn.storage_manager.database.repository;

import net.tmn.produce.database.jpa.ProduceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProduceTypeRepository extends JpaRepository<ProduceType, UUID> {
    Optional<ProduceType> findByName(String name);

    @Query("SELECT pt FROM ProduceType pt ORDER BY pt.name")
    List<ProduceType> findAllOrderByName();

    boolean existsByName(String name);
}