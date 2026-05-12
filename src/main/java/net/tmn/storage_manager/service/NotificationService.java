package net.tmn.storage_manager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.jpa.Notification;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.NotificationStatus;
import net.tmn.storage_manager.database.jpa.type.NotificationTargetType;
import net.tmn.storage_manager.database.jpa.type.NotificationType;
import net.tmn.storage_manager.database.repository.ItemInstanceRepository;
import net.tmn.storage_manager.database.repository.NotificationRepository;
import net.tmn.storage_manager.database.repository.StorageBoxRepository;
import net.tmn.storage_manager.service.validation.DomainValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    ItemInstanceRepository itemInstanceRepository;
    StorageBoxRepository storageBoxRepository;
    DomainValidator domainValidator;

    @NonFinal
    @Value("${app.notification.console.enabled:true}")
    boolean consoleNotificationEnabled;

    @NonFinal
    @Value("${app.notification.home-assistant.enabled:false}")
    boolean homeAssistantEnabled;

    @NonFinal
    @Value("${app.notification.sms.enabled:false}")
    boolean smsEnabled;

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications() {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<NotificationDisplay> getNotificationDisplays() {
        return notificationRepository.findAll().stream()
                .sorted((left, right) -> compareCreatedAtDescending(left, right))
                .map(notification -> new NotificationDisplay(notification, resolveTargetDisplay(notification)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByItemInstance(UUID itemInstanceId) {
        return notificationRepository.findByTargetTypeAndTargetId(NotificationTargetType.ITEM_INSTANCE, itemInstanceId);
    }

    @Transactional
    public void checkAndCreateNotifications() {
        log.debug("Checking for items requiring notifications...");

        // Check for expired items
        List<ItemInstance> expiredInstances = itemInstanceRepository.findInstancesExpiredBefore(LocalDate.now());
        expiredInstances.forEach(this::createExpiredNotification);

        // Check for items expiring soon
        List<ItemInstance> activeInstances = itemInstanceRepository.findAllActiveInstances();
        activeInstances.forEach(this::createExpiryWarningNotification);

        log.debug("Notification check completed");
    }

    @Transactional
    public void processPendingNotifications() {
        List<Notification> pendingNotifications =
                notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.PENDING);

        for (Notification notification : pendingNotifications) {
            try {
                boolean sent = sendNotification(notification);
                if (sent) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                }
                saveValidated(notification);
            } catch (Exception e) {
                log.error("Failed to send notification: {}", notification.getId(), e);
                notification.setStatus(NotificationStatus.FAILED);
                saveValidated(notification);
            }
        }
    }

    private void createExpiredNotification(ItemInstance instance) {
        // Check if notification already exists
        boolean notificationExists = notificationRepository.existsByTargetTypeAndTargetIdAndNotificationTypeAndStatus(
                NotificationTargetType.ITEM_INSTANCE,
                instance.getId(),
                NotificationType.EXPIRED,
                NotificationStatus.PENDING);

        if (!notificationExists) {
            Notification notification = new Notification();
            notification.setTargetType(NotificationTargetType.ITEM_INSTANCE);
            notification.setTargetId(instance.getId());
            notification.setNotificationType(NotificationType.EXPIRED);
            notification.setMessage("Item '%s' has expired (best before: %s)"
                    .formatted(instance.getTitle(), instance.getBestBeforeDate()));
            notification.setStatus(NotificationStatus.PENDING);

            saveValidated(notification);
            log.info("Created expired notification for item: {}", instance.getTitle());
        }
    }

    private void createExpiryWarningNotification(ItemInstance instance) {
        ItemType itemType = instance.getItemType();
        if (itemType.getNotificationDaysModifier() == null || itemType.getNotificationDaysModifier() <= 0) {
            return;
        }

        LocalDate warningDate = instance.getBestBeforeDate().minusDays(itemType.getNotificationDaysModifier());

        if (LocalDate.now().isAfter(warningDate) || LocalDate.now().equals(warningDate)) {
            // Check if notification already exists
            boolean notificationExists =
                    notificationRepository.existsByTargetTypeAndTargetIdAndNotificationTypeAndStatus(
                            NotificationTargetType.ITEM_INSTANCE,
                            instance.getId(),
                            NotificationType.EXPIRY_WARNING,
                            NotificationStatus.PENDING);

            if (!notificationExists) {
                Notification notification = new Notification();
                notification.setTargetType(NotificationTargetType.ITEM_INSTANCE);
                notification.setTargetId(instance.getId());
                notification.setNotificationType(NotificationType.EXPIRY_WARNING);
                notification.setMessage("Item '%s' will expire on %s (%d days)"
                        .formatted(
                                instance.getTitle(),
                                instance.getBestBeforeDate(),
                                java.time.temporal.ChronoUnit.DAYS.between(
                                        LocalDate.now(), instance.getBestBeforeDate())));
                notification.setStatus(NotificationStatus.PENDING);

                saveValidated(notification);
                log.info("Created expiry warning notification for item: {}", instance.getTitle());
            }
        }
    }

    private void saveValidated(Notification notification) {
        domainValidator.validate(notification);
        notificationRepository.save(notification);
    }

    private String resolveTargetDisplay(Notification notification) {
        if (notification.getTargetType() == NotificationTargetType.ITEM_INSTANCE) {
            return itemInstanceRepository
                    .findById(notification.getTargetId())
                    .map(ItemInstance::getTitle)
                    .orElse("Missing target");
        }

        if (notification.getTargetType() == NotificationTargetType.STORAGE_BOX) {
            return storageBoxRepository
                    .findById(notification.getTargetId())
                    .map(StorageBox::getBoxNumber)
                    .map(boxNumber -> "Storage Box #" + boxNumber)
                    .orElse("Missing target");
        }

        return "Unknown target";
    }

    private int compareCreatedAtDescending(Notification left, Notification right) {
        if (left.getCreatedAt() == null && right.getCreatedAt() == null) {
            return 0;
        }
        if (left.getCreatedAt() == null) {
            return 1;
        }
        if (right.getCreatedAt() == null) {
            return -1;
        }
        return right.getCreatedAt().compareTo(left.getCreatedAt());
    }

    private boolean sendNotification(Notification notification) {
        boolean sent = false;

        if (consoleNotificationEnabled) {
            sendConsoleNotification(notification);
            sent = true;
        }

        if (homeAssistantEnabled) {
            sent = sendHomeAssistantNotification(notification) || sent;
        }

        if (smsEnabled) {
            sent = sendSmsNotification(notification) || sent;
        }

        return sent;
    }

    private void sendConsoleNotification(Notification notification) {
        log.info("🔔 NOTIFICATION: {} - {}", notification.getNotificationType(), notification.getMessage());
    }

    private boolean sendHomeAssistantNotification(Notification notification) {
        // TODO: Implement Home Assistant API integration
        log.info("Would send Home Assistant notification: {}", notification.getMessage());
        return false;
    }

    private boolean sendSmsNotification(Notification notification) {
        // TODO: Implement SMS notification (e.g., Twilio)
        log.info("Would send SMS notification: {}", notification.getMessage());
        return false;
    }
}
