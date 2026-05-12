package net.tmn.storage_manager.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduledTaskService {

    ItemInstanceService itemInstanceService;
    NotificationService notificationService;

    @Scheduled(fixedRate = 600000) // Run every 10 minutes
    public void checkExpiredItems() {
        log.debug("Running scheduled check for expired items...");
        try {
            itemInstanceService.updateExpiredInstances();
            notificationService.checkAndCreateNotifications();
            notificationService.processPendingNotifications();
        } catch (Exception e) {
            log.error("Error during scheduled item check", e);
        }
    }
}
