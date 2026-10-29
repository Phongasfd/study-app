package com.namphong.backend;

import com.namphong.backend.entity.*;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the switch from ddl-auto: update to Flyway, on a real PostgreSQL 16.
 * If an entity changes without a matching migration, the validate step here fails
 * (the same check prod runs at startup with ddl-auto: validate).
 */
class SchemaMigrationTest {

    private static final Class<?>[] ENTITIES = {
            AccountEntity.class, DailyStat.class, GroupMember.class, GroupRanking.class, RefreshToken.class,
            SessionSubject.class, StudyGroup.class, StudySession.class, Subject.class, UserEntity.class
    };

    // same settings as config/FlywayConfig
    private static Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    /** Builds a Hibernate SessionFactory with the given hbm2ddl action (validate / create). */
    private static void runHibernate(DataSource dataSource, String hbm2ddlAction) {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.datasource", dataSource)
                .applySetting("hibernate.hbm2ddl.auto", hbm2ddlAction)
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.implicit_naming_strategy", "org.springframework.boot.hibernate.SpringImplicitNamingStrategy")
                .build();
        MetadataSources sources = new MetadataSources(registry);
        for (Class<?> entity : ENTITIES) {
            sources.addAnnotatedClass(entity);
        }
        try (SessionFactory ignored = sources.buildMetadata().buildSessionFactory()) {
            // building it is the check: validate throws SchemaManagementException on any mismatch
        }
    }

    private static boolean indexExists(DataSource dataSource, String index) throws Exception {
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.createStatement().executeQuery("select 1 from pg_indexes where indexname = '" + index + "'")) {
            return rs.next();
        }
    }

    @Test
    void freshDatabase_migrationsProduceTheSchemaTheEntitiesExpect() throws Exception {
        DataSource dataSource = EmbeddedPostgresSupport.dataSource(EmbeddedPostgresSupport.freshDatabaseUrl("fresh"));

        MigrateResult result = flyway(dataSource).migrate();

        assertEquals(2, result.migrationsExecuted, "V1 baseline schema + V2 index");
        assertDoesNotThrow(() -> runHibernate(dataSource, "validate"));
        assertTrue(indexExists(dataSource, "idx_study_sessions_user_status_start"));
    }

    @Test
    void existingDatabaseFromDdlAutoUpdate_isBaselinedAndOnlyGetsNewMigrations() throws Exception {
        // Simulates prod today: tables created by Hibernate, no flyway_schema_history
        DataSource dataSource = EmbeddedPostgresSupport.dataSource(EmbeddedPostgresSupport.freshDatabaseUrl("legacy"));
        runHibernate(dataSource, "create");

        MigrateResult result = flyway(dataSource).migrate();

        assertEquals(1, result.migrationsExecuted, "V1 must be skipped (baselined), only V2 runs");
        assertEquals("2", result.targetSchemaVersion);
        assertDoesNotThrow(() -> runHibernate(dataSource, "validate"));
        assertTrue(indexExists(dataSource, "idx_study_sessions_user_status_start"));
    }
}
