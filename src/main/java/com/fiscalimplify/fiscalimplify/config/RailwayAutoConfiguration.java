package com.fiscalimplify.fiscalimplify.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@AutoConfiguration
@AutoConfigureBefore(name = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@Conditional(RailwayDeploymentCondition.class)
public class RailwayAutoConfiguration {

    @Bean(name = "dataSource")
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(Environment environment) {
        RailwayDbConnectionDetails db = RailwayDbConnectionDetails.from(environment);

        HikariConfig config = new HikariConfig();
        config.setPoolName("FiscalimplifyHikariPool");
        config.setJdbcUrl(db.jdbcUrl());
        config.setUsername(db.username());
        config.setPassword(db.password());
        config.setMaximumPoolSize(environment.getProperty("HIKARI_MAX_POOL_SIZE", Integer.class, 4));
        config.setMinimumIdle(environment.getProperty("HIKARI_MIN_IDLE", Integer.class, 1));
        config.setConnectionTimeout(15_000);
        config.setInitializationFailTimeout(60_000);
        config.setMaxLifetime(600_000);
        config.setIdleTimeout(120_000);

        System.err.println("[Fiscalimplify] Railway DataSource: " + db.jdbcUrl());
        return new HikariDataSource(config);
    }
}
