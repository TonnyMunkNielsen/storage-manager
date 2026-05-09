package net.tmn.storage_manager.database.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import net.tmn.storage_manager.database.jpa.Notification;
import net.tmn.storage_manager.database.jpa.type.NotificationStatus;
import net.tmn.storage_manager.database.jpa.type.NotificationTargetType;
import net.tmn.storage_manager.database.jpa.type.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByTargetTypeAndTargetId(NotificationTargetType type, UUID targetId);

    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt <= :before ORDER BY n.createdAt")
    List<Notification> findPendingNotificationsCreatedBefore(@Param("before") LocalDateTime before);

    boolean existsByTargetTypeAndTargetIdAndNotificationTypeAndStatus(
            NotificationTargetType type, UUID targetId, NotificationType notificationType, NotificationStatus status);
}
