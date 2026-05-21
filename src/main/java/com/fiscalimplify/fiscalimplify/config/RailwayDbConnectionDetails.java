package com.fiscalimplify.fiscalimplify.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class RailwayDbConnectionDetails {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private RailwayDbConnectionDetails(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public static RailwayDbConnectionDetails from(Environment environment) {
        String configuredUrl = trim(environment.getProperty("spring.datasource.url"));
        if (RailwayDeployment.isValidJdbcUrl(configuredUrl)
                && !RailwayDeployment.isLocalhostJdbcUrl(configuredUrl)) {
            String username = trim(RailwayDeployment.firstNonBlank(
                    environment.getProperty("spring.datasource.username"),
                    environment.getProperty("PGUSER"),
                    environment.getProperty("DB_USERNAME"),
                    "postgres"
            ));
            String password = RailwayDeployment.firstNonBlank(
                    environment.getProperty("spring.datasource.password"),
                    environment.getProperty("PGPASSWORD"),
                    environment.getProperty("DB_PASSWORD"),
                    ""
            );
            return new RailwayDbConnectionDetails(configuredUrl, username, password);
        }

        String host = RailwayDeployment.postgresHost(environment);
        if (RailwayDeployment.isRemotePostgresHost(host)) {
            return new RailwayDbConnectionDetails(
                    "jdbc:postgresql://" + host + ":" + RailwayDeployment.postgresPort(environment)
                            + "/" + RailwayDeployment.postgresDatabase(environment),
                    RailwayDeployment.postgresUsername(environment),
                    RailwayDeployment.postgresPassword(environment)
            );
        }

        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_PRIVATE_URL"),
                environment.getProperty("DATABASE_URL")
        );
        if (!StringUtils.hasText(databaseUrl)) {
            throw new IllegalStateException(
                    "Postgres nao vinculado ao servico fiscalimplify no Railway. "
                            + RailwayDeployment.describeDatabaseEnv(environment)
                            + " -> Variables -> Add Reference -> Postgres (PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD).");
        }
        return fromDatabaseUrl(environment, databaseUrl);
    }

    private static RailwayDbConnectionDetails fromDatabaseUrl(Environment environment, String databaseUrl) {
        try {
            String normalized = databaseUrl.trim();
            if (normalized.startsWith("postgres://")) {
                normalized = "postgresql://" + normalized.substring("postgres://".length());
            }
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String database = (path != null && path.length() > 1) ? path.substring(1) : "railway";

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (StringUtils.hasText(userInfo)) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    username = decode(userInfo.substring(0, colon));
                    password = decode(userInfo.substring(colon + 1));
                } else {
                    username = decode(userInfo);
                }
            }

            username = trim(firstNonBlank(username, environment.getProperty("PGUSER"), "postgres"));
            password = firstNonBlank(password, environment.getProperty("PGPASSWORD"), "");

            return new RailwayDbConnectionDetails(
                    "jdbc:postgresql://" + host + ":" + port + "/" + database,
                    username,
                    password
            );
        } catch (Exception ex) {
            throw new IllegalStateException("DATABASE_URL invalida: " + ex.getMessage(), ex);
        }
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
