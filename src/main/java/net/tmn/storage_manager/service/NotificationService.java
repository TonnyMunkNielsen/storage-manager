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
import net.tmn.storage_manager.database.jpa.Notification;
import net.tmn.storage_manager.database.jpa.ProduceInstance;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.database.jpa.type.NotificationStatus;
import net.tmn.storage_manager.database.jpa.type.NotificationTargetType;
import net.tmn.storage_manager.database.jpa.type.NotificationType;
import net.tmn.storage_manager.database.repository.NotificationRepository;
import net.tmn.storage_manager.database.repository.ProduceInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    ProduceInstanceRepository produceInstanceRepository;

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
    public List<Notification> getNotificationsByProduceInstance(UUID produceInstanceId) {
        return notificationRepository.findByTargetTypeAndTargetId(
                NotificationTargetType.PRODUCE_INSTANCE, produceInstanceId);
    }

    @Transactional
    public void checkAndCreateNotifications() {
        log.debug("Checking for produces requiring notifications...");

        // Check for expired produces
        List<ProduceInstance> expiredInstances = produceInstanceRepository.findInstancesExpiredBefore(LocalDate.now());
        expiredInstances.forEach(this::createExpiredNotification);

        // Check for produces expiring soon
        List<ProduceInstance> activeInstances = produceInstanceRepository.findAllActiveInstances();
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
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.error("Failed to send notification: {}", notification.getId(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);
            }
        }
    }

    private void createExpiredNotification(ProduceInstance instance) {
        // Check if notification already exists
        boolean notificationExists = notificationRepository.existsByTargetTypeAndTargetIdAndNotificationTypeAndStatus(
                NotificationTargetType.PRODUCE_INSTANCE,
                instance.getId(),
                NotificationType.EXPIRED,
                NotificationStatus.PENDING);

        if (!notificationExists) {
            Notification notification = new Notification();
            notification.setTargetType(NotificationTargetType.PRODUCE_INSTANCE);
            notification.setTargetId(instance.getId());
            notification.setNotificationType(NotificationType.EXPIRED);
            notification.setMessage("Produce '%s' has expired (best before: %s)"
                    .formatted(instance.getTitle(), instance.getBestBeforeDate()));
            notification.setStatus(NotificationStatus.PENDING);

            notificationRepository.save(notification);
            log.info("Created expired notification for produce: {}", instance.getTitle());
        }
    }

    private void createExpiryWarningNotification(ProduceInstance instance) {
        ProduceType produceType = instance.getProduceType();
        if (produceType.getNotificationDaysModifier() == null || produceType.getNotificationDaysModifier() <= 0) {
            return;
        }

        LocalDate warningDate = instance.getBestBeforeDate().minusDays(produceType.getNotificationDaysModifier());

        if (LocalDate.now().isAfter(warningDate) || LocalDate.now().equals(warningDate)) {
            // Check if notification already exists
            boolean notificationExists =
                    notificationRepository.existsByTargetTypeAndTargetIdAndNotificationTypeAndStatus(
                            NotificationTargetType.PRODUCE_INSTANCE,
                            instance.getId(),
                            NotificationType.EXPIRY_WARNING,
                            NotificationStatus.PENDING);

            if (!notificationExists) {
                Notification notification = new Notification();
                notification.setTargetType(NotificationTargetType.PRODUCE_INSTANCE);
                notification.setTargetId(instance.getId());
                notification.setNotificationType(NotificationType.EXPIRY_WARNING);
                notification.setMessage("Produce '%s' will expire on %s (%d days)"
                        .formatted(
                                instance.getTitle(),
                                instance.getBestBeforeDate(),
                                java.time.temporal.ChronoUnit.DAYS.between(
                                        LocalDate.now(), instance.getBestBeforeDate())));
                notification.setStatus(NotificationStatus.PENDING);

                notificationRepository.save(notification);
                log.info("Created expiry warning notification for produce: {}", instance.getTitle());
            }
        }
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
