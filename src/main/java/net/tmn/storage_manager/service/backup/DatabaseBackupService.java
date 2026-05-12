package net.tmn.storage_manager.service.backup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.backup.directory:database_backups}")
    private String backupDirectory;

    @Value("${app.backup.retention.days:30}")
    private int retentionDays;

    @Value("${app.backup.include-flyway-history:true}")
    private boolean includeFlywayHistory;

    /**
     * Creates a backup of the database asynchronously
     */
    @Async
    public CompletableFuture<BackupResult> createBackup() {
        log.info("Starting database backup...");

        try {
            // Parse database connection details from URL
            DatabaseConnectionInfo connectionInfo = parseConnectionInfo(datasourceUrl);

            // Create backup directory if it doesn't exist
            Path backupDir = Path.of(backupDirectory);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // Generate timestamped filename
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String fileName = backupDirectory + "/backup_" + timestamp + ".sql";
            String encryptedFileName = backupDirectory + "/backup_" + timestamp + ".sql.enc";
            String keyFilePath = backupDirectory + "/aes.key";

            // Get or generate encryption key
            Key encryptionKey = getOrCreateEncryptionKey(keyFilePath);

            // Execute pg_dump
            boolean backupSuccess = executePgDump(connectionInfo, fileName);

            if (backupSuccess) {
                // Encrypt the backup file
                encryptFile(fileName, encryptedFileName, encryptionKey);

                // Delete unencrypted file
                Files.deleteIfExists(Path.of(fileName));

                // Clean up old backups
                cleanupOldBackups();

                log.info("Database backup completed successfully: {}", encryptedFileName);
                return CompletableFuture.completedFuture(new BackupResult(true, encryptedFileName, null));
            } else {
                return CompletableFuture.completedFuture(new BackupResult(false, null, "pg_dump failed"));
            }

        } catch (Exception e) {
            log.error("Database backup failed", e);
            return CompletableFuture.completedFuture(new BackupResult(false, null, e.getMessage()));
        }
    }

    /**
     * Restores the database from a backup file
     */
    public RestoreResult restoreFromBackup(String backupFilePath) {
        log.info("Starting database restore from: {}", backupFilePath);

        try {
            DatabaseConnectionInfo connectionInfo = parseConnectionInfo(datasourceUrl);
            String keyFilePath = backupDirectory + "/aes.key";
            String decryptedFilePath = backupDirectory + "/temp_restore_" + System.currentTimeMillis() + ".sql";

            // Load encryption key
            Key encryptionKey = loadKey(keyFilePath);

            // Decrypt backup file
            decryptFile(backupFilePath, decryptedFilePath, encryptionKey);

            // Apply SQL dump
            boolean restoreSuccess = executePsql(connectionInfo, decryptedFilePath);

            // Clean up temporary file
            Files.deleteIfExists(Path.of(decryptedFilePath));

            if (restoreSuccess) {
                log.info("Database restore completed successfully");
                return new RestoreResult(true, null);
            } else {
                return new RestoreResult(false, "psql execution failed");
            }

        } catch (Exception e) {
            log.error("Database restore failed", e);
            return new RestoreResult(false, e.getMessage());
        }
    }

    /**
     * Lists available backup files
     */
    public List<BackupInfo> listBackups() {
        try {
            Path backupDir = Path.of(backupDirectory);
            if (!Files.exists(backupDir)) {
                return List.of();
            }

            return Files.list(backupDir)
                    .filter(path -> path.getFileName().toString().endsWith(".sql.enc"))
                    .map(this::createBackupInfo)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(BackupInfo::timestamp).reversed())
                    .toList();

        } catch (IOException e) {
            log.error("Failed to list backups", e);
            return List.of();
        }
    }

    /**
     * Finds the most recent backup file
     */
    public Optional<String> findMostRecentBackup() {
        return listBackups().stream().findFirst().map(BackupInfo::filePath);
    }

    private DatabaseConnectionInfo parseConnectionInfo(String url) {
        // Parse jdbc:postgresql://localhost:5432/storagemanagerdb
        String cleanUrl = url.replace("jdbc:postgresql://", "");
        String[] parts = cleanUrl.split("/");
        String[] hostPort = parts[0].split(":");
        String host = hostPort[0];
        String port = hostPort.length > 1 ? hostPort[1] : "5432";
        String database = parts[1].split("\\?")[0]; // Remove query parameters if any

        return new DatabaseConnectionInfo(host, port, database);
    }

    private Key getOrCreateEncryptionKey(String keyFilePath) throws Exception {
        Path keyFile = Path.of(keyFilePath);
        if (Files.exists(keyFile)) {
            return loadKey(keyFilePath);
        } else {
            log.warn("No AES key file found! Generating new key at: {}", keyFilePath);
            Key newKey = generateKey();
            saveKey(newKey, keyFilePath);
            return newKey;
        }
    }

    private boolean executePgDump(DatabaseConnectionInfo connectionInfo, String outputFile) throws Exception {
        List<String> command = new ArrayList<>(List.of(
                "pg_dump",
                "-h",
                connectionInfo.host(),
                "-p",
                connectionInfo.port(),
                "-U",
                username,
                "-d",
                connectionInfo.database(),
                "-n",
                "public"));

        if (!includeFlywayHistory) {
            command.add("-T");
            command.add("public.flyway_schema_history");
        }

        command.add("-f");
        command.add(outputFile);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PGPASSWORD", password);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // Log error output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("pg_dump error: {}", line);
                }
            }
        }

        return exitCode == 0;
    }

    private boolean executePsql(DatabaseConnectionInfo connectionInfo, String inputFile) throws Exception {
        List<String> command = List.of(
                "psql",
                "-h",
                connectionInfo.host(),
                "-p",
                connectionInfo.port(),
                "-U",
                username,
                "-d",
                connectionInfo.database(),
                "-f",
                inputFile);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PGPASSWORD", password);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // Log error output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("psql error: {}", line);
                }
            }
        }

        return exitCode == 0;
    }

    private void encryptFile(String inputFilePath, String outputFilePath, Key key) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(inputFilePath);
                FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] inputBytes = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(inputBytes)) != -1) {
                byte[] outputBytes = cipher.update(inputBytes, 0, bytesRead);
                if (outputBytes != null) {
                    outputStream.write(outputBytes);
                }
            }
            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }
        }
    }

    private void decryptFile(String inputFilePath, String outputFilePath, Key key) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(inputFilePath);
                FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] inputBytes = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(inputBytes)) != -1) {
                byte[] outputBytes = cipher.update(inputBytes, 0, bytesRead);
                if (outputBytes != null) {
                    outputStream.write(outputBytes);
                }
            }
            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }
        }
    }

    private Key generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private void saveKey(Key key, String filePath) throws Exception {
        try (FileOutputStream keyStream = new FileOutputStream(filePath)) {
            keyStream.write(key.getEncoded());
        }
    }

    private Key loadKey(String filePath) throws Exception {
        try (FileInputStream keyStream = new FileInputStream(filePath)) {
            byte[] keyBytes = keyStream.readAllBytes();
            return new SecretKeySpec(keyBytes, "AES");
        }
    }

    private void cleanupOldBackups() {
        try {
            Path backupDir = Path.of(backupDirectory);
            if (!Files.exists(backupDir)) {
                return;
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            Files.list(backupDir)
                    .filter(path -> path.getFileName().toString().endsWith(".sql.enc"))
                    .filter(path -> {
                        Optional<LocalDateTime> timestamp =
                                extractTimestampFromFilename(path.getFileName().toString());
                        return timestamp.isPresent() && timestamp.get().isBefore(cutoffDate);
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Deleted old backup: {}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete old backup: {}", path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup old backups", e);
        }
    }

    private Optional<BackupInfo> createBackupInfo(Path path) {
        String fileName = path.getFileName().toString();
        Optional<LocalDateTime> timestamp = extractTimestampFromFilename(fileName);

        return timestamp.map(ts -> {
            try {
                long size = Files.size(path);
                return new BackupInfo(fileName, path.toString(), ts, size);
            } catch (IOException e) {
                log.warn("Failed to get file size for: {}", fileName, e);
                return new BackupInfo(fileName, path.toString(), ts, 0L);
            }
        });
    }

    private Optional<LocalDateTime> extractTimestampFromFilename(String fileName) {
        try {
            String baseName = fileName.substring(0, fileName.lastIndexOf(".sql.enc"));
            String timestamp = baseName.substring(baseName.indexOf('_') + 1);
            return Optional.of(LocalDateTime.parse(timestamp, DATE_FORMATTER));
        } catch (Exception e) {
            log.warn("Failed to parse timestamp from filename: {}", fileName, e);
            return Optional.empty();
        }
    }

    // Record classes for return types
    public record BackupResult(boolean success, String filePath, String errorMessage) {}

    public record RestoreResult(boolean success, String errorMessage) {}

    public record BackupInfo(String fileName, String filePath, LocalDateTime timestamp, long sizeBytes) {}

    private record DatabaseConnectionInfo(String host, String port, String database) {}
}
