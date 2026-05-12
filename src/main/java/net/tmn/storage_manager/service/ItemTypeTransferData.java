package net.tmn.storage_manager.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemTypeTransferData(int schemaVersion, Instant exportedAt, List<ItemTypeRecord> itemTypes) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ItemTypeTransferData {
        exportedAt = exportedAt == null ? Instant.now() : exportedAt;
        itemTypes = itemTypes == null ? List.of() : List.copyOf(itemTypes);
    }

    public ItemTypeTransferData(List<ItemTypeRecord> itemTypes) {
        this(CURRENT_SCHEMA_VERSION, Instant.now(), itemTypes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemTypeRecord(
            String name,
            String description,
            BigDecimal price,
            Integer notificationDaysModifier,
            String imageFilename,
            String imageContentType,
            byte[] imageData) {}
}
