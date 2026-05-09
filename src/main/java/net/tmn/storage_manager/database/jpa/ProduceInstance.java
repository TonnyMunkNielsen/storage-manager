package net.tmn.storage_manager.database.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.type.ProduceInstanceStatus;
import org.springframework.format.annotation.DateTimeFormat;

@Table
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProduceInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull(message = "Produce type is required")
    @JoinColumn(name = "produce_type_id", nullable = false, updatable = false)
    ProduceType produceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull(message = "Storage box is required")
    @JoinColumn(name = "storage_box_id", nullable = false)
    StorageBox storageBox;

    @NotBlank(message = "Title is required")
    @Column(name = "title", nullable = false)
    String title;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Best before date is required")
    @Column(name = "best_before_date", nullable = false)
    LocalDate bestBeforeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ProduceInstanceStatus status = ProduceInstanceStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    ProduceInstance replacedBy;

    @Column(name = "replaced_at")
    LocalDateTime replacedAt;

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

    public long getDaysRemaining() {
        if (bestBeforeDate == null) return 0;
        return ChronoUnit.DAYS.between(
                LocalDate.now(), bestBeforeDate.plusDays(produceType.getNotificationDaysModifier()));
    }
}
