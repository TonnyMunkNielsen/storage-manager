package net.tmn.storage_manager.service.backup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(name = "app.backup.scheduled.enabled", havingValue = "true")
public class ScheduledBackupService {

    DatabaseBackupService backupService;

    /**
     * Scheduled backup execution
     * Default: Every day at 2 AM
     */
    @Scheduled(cron = "${app.backup.scheduled.cron:0 0/15 * * * ?}")
    public void performScheduledBackup() {
        log.info(
                "Starting scheduled database backup at {}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            backupService
                    .createBackup()
                    .thenAccept(result -> {
                        if (result.success()) {
                            log.info("Scheduled backup completed successfully: {}", result.filePath());
                        } else {
                            log.error("Scheduled backup failed: {}", result.errorMessage());
                        }
                    })
                    .exceptionally(throwable -> {
                        log.error("Scheduled backup failed with exception", throwable);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to start scheduled backup", e);
        }
    }

    /**
     * Health check - logs backup service status
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void logBackupStatus() {
        try {
            var backups = backupService.listBackups();
            var mostRecent = backupService.findMostRecentBackup();

            log.debug(
                    "Backup status - Total backups: {}, Most recent: {}",
                    backups.size(),
                    mostRecent
                            .map(path -> path.substring(path.lastIndexOf('/') + 1))
                            .orElse("None"));
        } catch (Exception e) {
            log.warn("Failed to check backup status", e);
        }
    }
}
