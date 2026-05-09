package net.tmn.storage_manager.database.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

@Table
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotBlank(message = "Username is required")
    @Column(name = "username", nullable = false, unique = true)
    String username;

    @NotBlank(message = "Password hash is required")
    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @NotBlank(message = "Role is required")
    @Column(name = "role", nullable = false)
    String role;

    @ColumnDefault(value = "true")
    @Column(name = "enabled", nullable = false)
    Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
