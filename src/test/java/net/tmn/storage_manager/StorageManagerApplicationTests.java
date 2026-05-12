package net.tmn.storage_manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:storage-manager-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.docker.compose.enabled=false",
            "spring.task.scheduling.enabled=false",
            "app.backup.scheduled.enabled=false",
            "app.backup.directory=build/test-backups",
            "vaadin.launch-browser=false"
        })
class StorageManagerApplicationTests {

    @Test
    void contextLoads() {}
}
