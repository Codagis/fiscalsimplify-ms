package com.fiscalimplify.fiscalimplify.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Pool HTTP compartilhado e limitado para reduzir memória no deploy Railway.
 */
@Configuration
@Profile("railway")
public class RailwayRuntimeConfig {

    @Bean(destroyMethod = "dispose")
    public ConnectionProvider railwayConnectionProvider() {
        return ConnectionProvider.builder("fiscalimplify-http")
                .maxConnections(10)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofMinutes(3))
                .pendingAcquireMaxCount(20)
                .pendingAcquireTimeout(Duration.ofSeconds(8))
                .evictInBackground(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WebClient.Builder webClientBuilder(ConnectionProvider railwayConnectionProvider) {
        HttpClient httpClient = HttpClient.create(railwayConnectionProvider)
                .compress(true)
                .responseTimeout(Duration.ofSeconds(45));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
