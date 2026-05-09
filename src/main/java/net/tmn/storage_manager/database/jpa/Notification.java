package net.tmn.storage_manager.database.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.type.NotificationStatus;
import net.tmn.storage_manager.database.jpa.type.NotificationTargetType;
import net.tmn.storage_manager.database.jpa.type.NotificationType;

@Table
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "target_id", nullable = false, updatable = false)
    UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false)
    NotificationTargetType targetType;

    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    NotificationType notificationType;

    @NotBlank(message = "Message is required")
    @Column(name = "message", nullable = false)
    String message;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    NotificationStatus status = NotificationStatus.PENDING;

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
