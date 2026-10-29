package com.namphong.backend.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    // Databases created earlier by ddl-auto: update already have the V1 schema but no
    // flyway_schema_history table. baselineOnMigrate marks them as version 1 (V1 is skipped)
    // and then runs V2+. A fresh, empty database still runs V1 normally.
    // Set in code rather than YAML because application.yml (dev) is not in git.
    @Bean
    public FlywayConfigurationCustomizer baselineExistingSchema() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion("1");
    }
}
