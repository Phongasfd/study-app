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
import java.sql.SQLException;
import java.sql.Statement;

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

        assertEquals(3, result.migrationsExecuted, "V1 baseline schema + V2 index + V3 one running session");
        assertDoesNotThrow(() -> runHibernate(dataSource, "validate"));
        assertTrue(indexExists(dataSource, "idx_study_sessions_user_status_start"));
        assertTrue(indexExists(dataSource, "uq_study_sessions_one_running_per_user"));
    }

    @Test
    void existingDatabaseFromDdlAutoUpdate_isBaselinedAndOnlyGetsNewMigrations() throws Exception {
        // Simulates prod today: tables created by Hibernate, no flyway_schema_history
        DataSource dataSource = EmbeddedPostgresSupport.dataSource(EmbeddedPostgresSupport.freshDatabaseUrl("legacy"));
        runHibernate(dataSource, "create");

        MigrateResult result = flyway(dataSource).migrate();

        assertEquals(2, result.migrationsExecuted, "V1 must be skipped (baselined), only V2 and V3 run");
        assertEquals("3", result.targetSchemaVersion);
        assertDoesNotThrow(() -> runHibernate(dataSource, "validate"));
        assertTrue(indexExists(dataSource, "idx_study_sessions_user_status_start"));
        assertTrue(indexExists(dataSource, "uq_study_sessions_one_running_per_user"));
    }

    @Test
    void duplicateRunningSessionsAreClosedSoTheUniqueIndexCanBeBuilt() throws Exception {
        // Prod may already hold duplicates from the old race; the migration must not fail on them
        DataSource dataSource = EmbeddedPostgresSupport.dataSource(EmbeddedPostgresSupport.freshDatabaseUrl("dupes"));
        Flyway.configure().dataSource(dataSource).target("2").load().migrate(); // the schema before V3
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("insert into users (id, email, username) values ('00000000-0000-0000-0000-000000000001', 'a@x.io', 'a')");
            s.execute("insert into study_sessions (id, user_id, status, start_time) values " +
                    "('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-000000000001', 'RUNNING', '2026-11-01 09:00')," +
                    "('00000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-000000000001', 'RUNNING', '2026-11-01 09:00:01')");
        }

        flyway(dataSource).migrate();

        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "select id, status, duration_seconds from study_sessions order by start_time")) {
            assertTrue(rs.next());
            assertEquals("COMPLETED", rs.getString("status"), "older duplicate is closed");
            assertEquals(0, rs.getInt("duration_seconds"), "and credits nothing (it overlaps the kept one)");
            assertTrue(rs.next());
            assertEquals("RUNNING", rs.getString("status"), "latest session keeps running");
        }
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.execute(
                    "insert into study_sessions (id, user_id, status, start_time) values " +
                    "(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'RUNNING', now())"),
                    "a second RUNNING session is rejected by the database");
        }
    }
}
