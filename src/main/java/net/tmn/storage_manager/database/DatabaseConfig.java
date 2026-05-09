package net.tmn.storage_manager.database;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories
@EntityScan("net.tmn.storage_manager.database.jpa")
public class DatabaseConfig {}
