package com.fiscalimplify.fiscalimplify.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Garante spring.datasource.* a partir das variáveis do Postgres no Railway (PGHOST ou DATABASE_URL).
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE = "railwayDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isRailwayDeployment(environment)) {
            return;
        }

        String pgHost = environment.getProperty("PGHOST");
        if (StringUtils.hasText(pgHost) && !isLocalHost(pgHost)) {
            applyPostgresVariables(environment, pgHost);
            return;
        }

        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_PRIVATE_URL"),
                environment.getProperty("DATABASE_URL")
        );
        if (StringUtils.hasText(databaseUrl)) {
            applyDatabaseUrl(environment, databaseUrl);
        }
    }

    private static void applyPostgresVariables(ConfigurableEnvironment environment, String pgHost) {
        String port = environment.getProperty("PGPORT", "5432");
        String database = firstNonBlank(
                environment.getProperty("PGDATABASE"),
                environment.getProperty("DB_DATABASE"),
                "railway"
        );
        String username = firstNonBlank(
                environment.getProperty("PGUSER"),
                environment.getProperty("DB_USERNAME"),
                "postgres"
        );
        String password = firstNonBlank(
                environment.getProperty("PGPASSWORD"),
                environment.getProperty("DB_PASSWORD"),
                ""
        );

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:postgresql://" + pgHost + ":" + port + "/" + database);
        props.put("spring.datasource.username", username);
        props.put("spring.datasource.password", password);
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
    }

    private static void applyDatabaseUrl(ConfigurableEnvironment environment, String databaseUrl) {
        try {
            ParsedPostgres parsed = parsePostgresUrl(databaseUrl);
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", parsed.jdbcUrl());
            props.put("spring.datasource.username", firstNonBlank(
                    environment.getProperty("PGUSER"),
                    environment.getProperty("DB_USERNAME"),
                    parsed.username()
            ));
            props.put("spring.datasource.password", firstNonBlank(
                    environment.getProperty("PGPASSWORD"),
                    environment.getProperty("DB_PASSWORD"),
                    parsed.password()
            ));
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
        } catch (Exception ignored) {
            // YAML/variáveis PG* seguem como fallback
        }
    }

    private static boolean isRailwayDeployment(ConfigurableEnvironment environment) {
        if (StringUtils.hasText(environment.getProperty("RAILWAY_ENVIRONMENT"))
                || StringUtils.hasText(environment.getProperty("RAILWAY_PROJECT_ID"))
                || StringUtils.hasText(environment.getProperty("RAILWAY_SERVICE_ID"))) {
            return true;
        }
        String profiles = firstNonBlank(
                environment.getProperty("SPRING_PROFILES_ACTIVE"),
                environment.getProperty("spring.profiles.active")
        );
        if (profiles != null && profiles.contains("railway")) {
            return true;
        }
        return StringUtils.hasText(environment.getProperty("PGHOST"))
                || StringUtils.hasText(environment.getProperty("DATABASE_URL"))
                || StringUtils.hasText(environment.getProperty("DATABASE_PRIVATE_URL"));
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    static ParsedPostgres parsePostgresUrl(String databaseUrl) {
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

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        return new ParsedPostgres(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record ParsedPostgres(String jdbcUrl, String username, String password) {
    }
}
