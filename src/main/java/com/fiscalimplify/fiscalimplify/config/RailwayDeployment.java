package com.fiscalimplify.fiscalimplify.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Detecta deploy no Railway e variaveis Postgres (PGHOST ou DB_HOST).
 */
public final class RailwayDeployment {

    public static final String DATASOURCE_AUTO_CONFIG =
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration";

    private RailwayDeployment() {
    }

    public static boolean isRailwayDeployment(Environment environment) {
        if (StringUtils.hasText(environment.getProperty("RAILWAY_ENVIRONMENT"))
                || StringUtils.hasText(environment.getProperty("RAILWAY_PROJECT_ID"))
                || StringUtils.hasText(environment.getProperty("RAILWAY_SERVICE_ID"))
                || StringUtils.hasText(environment.getProperty("RAILWAY_DEPLOYMENT_ID"))) {
            return true;
        }
        String profiles = firstNonBlank(
                environment.getProperty("SPRING_PROFILES_ACTIVE"),
                environment.getProperty("spring.profiles.active")
        );
        if (profiles != null && profiles.contains("railway")) {
            return true;
        }
        return StringUtils.hasText(environment.getProperty("PORT"))
                && hasUnresolvedJdbcUrl(environment);
    }

    public static boolean hasRailwayPostgresConfig(Environment environment) {
        if (isRemotePostgresHost(postgresHost(environment))) {
            return true;
        }
        if (StringUtils.hasText(environment.getProperty("DATABASE_URL"))
                || StringUtils.hasText(environment.getProperty("DATABASE_PRIVATE_URL"))) {
            return true;
        }
        String url = environment.getProperty("spring.datasource.url");
        return isValidJdbcUrl(url) && !isLocalhostJdbcUrl(url);
    }

    public static String postgresHost(Environment environment) {
        return firstNonBlank(
                trimEnv(environment.getProperty("PGHOST")),
                trimEnv(environment.getProperty("DB_HOST"))
        );
    }

    public static String postgresPort(Environment environment) {
        return firstNonBlank(
                trimEnv(environment.getProperty("PGPORT")),
                trimEnv(environment.getProperty("DB_PORT")),
                "5432"
        );
    }

    public static String postgresDatabase(Environment environment) {
        return firstNonBlank(
                trimEnv(environment.getProperty("PGDATABASE")),
                trimEnv(environment.getProperty("DB_DATABASE")),
                "railway"
        );
    }

    public static String postgresUsername(Environment environment) {
        return firstNonBlank(
                trimEnv(environment.getProperty("PGUSER")),
                trimEnv(environment.getProperty("DB_USERNAME")),
                "postgres"
        );
    }

    public static String postgresPassword(Environment environment) {
        return firstNonBlank(
                environment.getProperty("PGPASSWORD"),
                environment.getProperty("DB_PASSWORD"),
                ""
        );
    }

    public static boolean isRemotePostgresHost(String host) {
        return StringUtils.hasText(host) && !isLocalHost(host);
    }

    public static boolean isValidJdbcUrl(String url) {
        return StringUtils.hasText(url)
                && url.startsWith("jdbc:postgresql://")
                && !url.contains("${");
    }

    public static boolean isLocalhostJdbcUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return url.contains("localhost") || url.contains("127.0.0.1");
    }

    public static boolean hasUnresolvedJdbcUrl(Environment environment) {
        String url = environment.getProperty("spring.datasource.url");
        return url != null && url.contains("${");
    }

    public static boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    public static String trimEnv(String value) {
        return value != null ? value.trim() : null;
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static String describeDatabaseEnv(Environment environment) {
        return "PGHOST=" + absentOr(environment.getProperty("PGHOST"))
                + ", DB_HOST=" + absentOr(environment.getProperty("DB_HOST"))
                + ", DATABASE_URL=" + absentOr(environment.getProperty("DATABASE_URL"))
                + ", spring.datasource.url=" + absentOr(environment.getProperty("spring.datasource.url"));
    }

    private static String absentOr(String value) {
        return StringUtils.hasText(value) ? "<definido>" : "<ausente>";
    }
}
