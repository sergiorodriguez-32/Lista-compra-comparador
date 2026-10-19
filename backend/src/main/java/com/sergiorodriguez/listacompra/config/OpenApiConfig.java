package com.sergiorodriguez.listacompra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura la documentación Swagger/OpenAPI: título de la API y el botón "Authorize" para
 * pegar el JWT y probar desde Swagger UI los endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lista de la compra comparador API")
                        .version("v1")
                        .description("API REST de la app de despensa y lista de la compra"))
                // Declara que se puede enviar un JWT en la cabecera Authorization: Bearer <token>
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Lo aplica a todos los endpoints (los públicos de /api/auth lo ignoran)
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
