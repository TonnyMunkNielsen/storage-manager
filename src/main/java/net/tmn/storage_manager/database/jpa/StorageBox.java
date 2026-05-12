package net.tmn.storage_manager.database.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.type.StorageBoxStatus;
import org.springframework.format.annotation.DateTimeFormat;

@Table
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageBox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull(message = "Box number is required")
    @Positive(message = "Box number must be positive")
    @Column(name = "box_number", nullable = false, updatable = false, unique = true)
    Integer boxNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Dessicant change date is required")
    @Column(name = "dessicant_changed_date", nullable = false)
    LocalDate dessicantChangedDate;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    @Column(name = "status", nullable = false)
    StorageBoxStatus status = StorageBoxStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "storageBox", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ItemInstance> itemInstances;

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
