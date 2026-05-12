package net.tmn.storage_manager.database.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

@Table(name = "item_type")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotBlank(message = "Name is required")
    @Column(unique = true, nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "price", precision = 8, scale = 2)
    BigDecimal price;

    @Column(name = "image_data", columnDefinition = "BYTEA")
    byte[] imageData;

    @Column(name = "image_content_type")
    String imageContentType;

    @Column(name = "image_filename")
    String imageFilename;

    @ColumnDefault(value = "0")
    @Column(name = "notification_days_modifier")
    Integer notificationDaysModifier;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "itemType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ItemInstance> instances;

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
