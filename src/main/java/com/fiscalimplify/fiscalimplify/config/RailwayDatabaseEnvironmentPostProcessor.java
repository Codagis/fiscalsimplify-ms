package com.fiscalimplify.fiscalimplify.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Executa por ultimo: sobrescreve spring.datasource.* com URL real (PGHOST / DATABASE_URL).
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE = "railwayDatabaseUrl";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean railway = RailwayDeployment.isRailwayDeployment(environment);
        boolean badUrl = RailwayDeployment.hasUnresolvedJdbcUrl(environment);
        if (!railway && !badUrl) {
            return;
        }

        if (badUrl) {
            System.err.println("[Fiscalimplify] JDBC com placeholders detectada - reconfigurando a partir de PGHOST/DATABASE_URL");
        }

        if (applyFromPostgresEnv(environment) || applyFromDatabaseUrl(environment)) {
            excludeDataSourceAutoConfig(environment);
            return;
        }

        if (badUrl) {
            throw new IllegalStateException(
                    "JDBC invalida no Railway. Variables -> Add Reference -> Postgres (PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD). "
                            + "Apague SPRING_DATASOURCE_URL manual se existir.");
        }

        if (railway && !RailwayDeployment.hasRailwayPostgresConfig(environment)) {
            throw new IllegalStateException(
                    "[Fiscalimplify] Postgres nao vinculado ao servico fiscalimplify. "
                            + RailwayDeployment.describeDatabaseEnv(environment)
                            + " -> Railway Variables -> Add Reference -> Postgres.");
        }
    }

    private static boolean applyFromPostgresEnv(ConfigurableEnvironment environment) {
        String host = RailwayDeployment.postgresHost(environment);
        if (!RailwayDeployment.isRemotePostgresHost(host)) {
            return false;
        }
        publish(
                environment,
                "jdbc:postgresql://" + host + ":" + RailwayDeployment.postgresPort(environment)
                        + "/" + RailwayDeployment.postgresDatabase(environment),
                RailwayDeployment.postgresUsername(environment),
                RailwayDeployment.postgresPassword(environment)
        );
        return true;
    }

    private static boolean applyFromDatabaseUrl(ConfigurableEnvironment environment) {
        String databaseUrl = RailwayDeployment.firstNonBlank(
                environment.getProperty("DATABASE_PRIVATE_URL"),
                environment.getProperty("DATABASE_URL")
        );
        if (!StringUtils.hasText(databaseUrl)) {
            return false;
        }
        try {
            ParsedPostgres parsed = parsePostgresUrl(databaseUrl);
            String username = RailwayDeployment.firstNonBlank(
                    environment.getProperty("PGUSER"),
                    environment.getProperty("DB_USERNAME"),
                    parsed.username()
            );
            String password = RailwayDeployment.firstNonBlank(
                    environment.getProperty("PGPASSWORD"),
                    environment.getProperty("DB_PASSWORD"),
                    parsed.password()
            );
            publish(environment, parsed.jdbcUrl(), username, password);
            return true;
        } catch (Exception ex) {
            System.err.println("[Fiscalimplify] DATABASE_URL invalida: " + ex.getMessage());
            return false;
        }
    }

    private static void excludeDataSourceAutoConfig(ConfigurableEnvironment environment) {
        Map<String, Object> bootstrap = new HashMap<>();
        bootstrap.put("spring.autoconfigure.exclude[0]", RailwayDeployment.DATASOURCE_AUTO_CONFIG);
        environment.getPropertySources().addFirst(new MapPropertySource("railwayBootstrap", bootstrap));
    }

    private static void publish(ConfigurableEnvironment environment, String url, String username, String password) {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", url);
        props.put("spring.datasource.username", username);
        props.put("spring.datasource.password", password);
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
        System.setProperty("spring.datasource.url", url);
        System.setProperty("spring.datasource.username", username);
        if (password != null) {
            System.setProperty("spring.datasource.password", password);
        }
        System.err.println("[Fiscalimplify] Datasource: " + url);
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

        return new ParsedPostgres("jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record ParsedPostgres(String jdbcUrl, String username, String password) {
    }
}
