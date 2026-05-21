package com.fiscalimplify.fiscalimplify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient padrão fora do perfil railway (desenvolvimento local).
 */
@Configuration
@Profile("!railway")
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
