package net.tmn.storage_manager.database.repository;

import java.util.Optional;
import java.util.UUID;
import net.tmn.storage_manager.database.jpa.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
