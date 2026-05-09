package net.tmn.storage_manager.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProduceTypeTransferData(int schemaVersion, Instant exportedAt, List<ProduceTypeRecord> produceTypes) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ProduceTypeTransferData {
        exportedAt = exportedAt == null ? Instant.now() : exportedAt;
        produceTypes = produceTypes == null ? List.of() : List.copyOf(produceTypes);
    }

    public ProduceTypeTransferData(List<ProduceTypeRecord> produceTypes) {
        this(CURRENT_SCHEMA_VERSION, Instant.now(), produceTypes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProduceTypeRecord(
            String name,
            String description,
            BigDecimal price,
            Integer notificationDaysModifier,
            String imageFilename,
            String imageContentType,
            byte[] imageData) {}
}
