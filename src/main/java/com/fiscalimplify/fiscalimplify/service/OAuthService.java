package com.fiscalimplify.fiscalimplify.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Serviço de autenticação OAuth2 (client_credentials) com a API Nuvem Fiscal.
 * Gerencia o token de acesso e renova automaticamente antes do vencimento.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Service
@Slf4j
public class OAuthService {

    private static final int MARGEM_SEGUNDOS = 60;

    private final ObjectProvider<WebClient.Builder> webClientBuilderProvider;

    private WebClient authClient;

    public OAuthService(ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        this.webClientBuilderProvider = webClientBuilderProvider;
    }

    @Value("${nuvemfiscal.auth-url}")
    private String authUrl;

    @Value("${nuvemfiscal.client-id}")
    private String clientId;

    @Value("${nuvemfiscal.client-secret}")
    private String clientSecret;

    @Value("${nuvemfiscal.scopes:empresa nfe nfce distribuicao-nfe}")
    private String scopes;

    private volatile String token;
    private volatile LocalDateTime expiresAt;

    @PostConstruct
    void initAuthClient() {
        authClient = webClientBuilderProvider.getObject().baseUrl(authUrl).build();
    }

    public synchronized String getToken() {
        if (token != null && expiresAt != null && expiresAt.isAfter(LocalDateTime.now())) {
            return token;
        }
        return obterNovoToken();
    }

    private String obterNovoToken() {
        log.debug("Obtendo novo token OAuth2 na Nuvem Fiscal (sandbox)");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("scope", scopes != null && !scopes.isBlank() ? scopes.trim() : "empresa nfe nfce");

        Map<?, ?> response = authClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Resposta nula ao obter token OAuth2");
        }

        token = (String) response.get("access_token");
        Object expiresInObj = response.get("expires_in");
        int expiresIn = (expiresInObj instanceof Number n) ? n.intValue() : 3600;

        expiresAt = LocalDateTime.now().plusSeconds(expiresIn - MARGEM_SEGUNDOS);
        log.debug("Token obtido com sucesso, expira em {} segundos", expiresIn);

        return token;
    }
}
