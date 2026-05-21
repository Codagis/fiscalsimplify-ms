package com.fiscalimplify.fiscalimplify.config;

import com.fiscalimplify.fiscalimplify.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuração responsável pelo bean WebClient para comunicação com a API Nuvem Fiscal.
 * Inclui interceptor OAuth2 e timeouts.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Configuration
@RequiredArgsConstructor
public class NuvemFiscalConfig {

    private final OAuthService oauthService;
    private final WebClient.Builder webClientBuilder;

    @Value("${nuvemfiscal.base-url}")
    private String baseUrl;

    @Value("${nuvemfiscal.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${nuvemfiscal.read-timeout:30000}")
    private int readTimeout;

    @Bean("nuvemFiscalClient")
    public WebClient nuvemFiscalClient() {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .filter((request, next) -> {
                    ClientRequest newRequest = ClientRequest.from(request)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oauthService.getToken())
                            .build();
                    return next.exchange(newRequest);
                })
                .build();
    }
}
