package com.fiscalimplify.fiscalimplify.documentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do OpenAPI 3.0 / Swagger para documentação da API Fiscal Simplify.
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-03-05
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "apiKey";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(new Server().url("/").description("API Base")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("X-API-Key")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("API Key para autenticação. Header: X-API-Key ou Authorization: ApiKey {key}")));
    }

    private Info apiInfo() {
        return new Info()
                .title("Fiscal Simplify API")
                .description("""
                        API de documentos fiscais - NFC-e, NF-e e integração com Nuvem Fiscal.
                        
                        ## Autenticação
                        Requer API Key no header:
                        ```
                        X-API-Key: {sua-api-key}
                        ```
                        Ou: `Authorization: ApiKey {sua-api-key}`
                        
                        ## Endpoints Protegidos
                        - `/companies/**` - Empresas
                        - `/nfce/**` - NFC-e
                        - `/nfe/**` - NF-e
                        
                        ## Webhook (se configurado)
                        - `/webhook/nuvemfiscal` - Recebe callbacks da Nuvem Fiscal (header X-Webhook-Secret)
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("VendaLume")
                        .email("contato@vendalume.com.br")
                        .url("https://vendalume.com.br"))
                .license(new License()
                        .name("Proprietário")
                        .url("https://vendalume.com.br/license"));
    }
}
