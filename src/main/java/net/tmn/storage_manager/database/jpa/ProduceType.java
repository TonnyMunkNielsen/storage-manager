package net.tmn.storage_manager.database.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProduceType {
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

    @OneToMany(mappedBy = "produceType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ProduceInstance> instances;

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