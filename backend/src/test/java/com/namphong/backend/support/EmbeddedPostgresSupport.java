package com.namphong.backend.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One real PostgreSQL 16 process per test JVM (zonky embedded-postgres: no Docker needed).
 * Each test asks for its own fresh database so tests never see each other's data.
 */
public final class EmbeddedPostgresSupport {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static EmbeddedPostgres postgres;

    private EmbeddedPostgresSupport() {
    }

    private static synchronized EmbeddedPostgres instance() {
        if (postgres == null) {
            try {
                postgres = EmbeddedPostgres.start();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                    // JVM is exiting anyway
                }
            }));
        }
        return postgres;
    }

    /** Creates an empty database and returns its JDBC URL (user/password: postgres/postgres). */
    public static String freshDatabaseUrl(String prefix) {
        String name = prefix + "_" + COUNTER.incrementAndGet();
        try (Connection c = instance().getPostgresDatabase().getConnection();
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create database " + name, e);
        }
        return instance().getJdbcUrl("postgres", name);
    }

    public static DataSource dataSource(String jdbcUrl) {
        org.postgresql.ds.PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
        ds.setUrl(jdbcUrl);
        ds.setUser("postgres");
        ds.setPassword("postgres");
        return ds;
    }
}
