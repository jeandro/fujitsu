package com.factory.events.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (springdoc) do contrato da API de Eventos de Fábrica.
 * <p>
 * Fonte da verdade: {@code docs/api/openapi.yaml} (OpenAPI 3.1). O bean abaixo expõe
 * {@code info}, {@code servers}, {@code tags} e {@code securitySchemes} — o Swagger UI em
 * {@code /swagger-ui.html} e o documento em {@code /v3/api-docs} são derivados daqui.
 * Anotações Swagger ({@code io.swagger.v3.oas.annotations}) só existem em
 * {@code adapter.in.web}; este é o único ponto de configuração do contrato.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme bearerAuth = new SecurityScheme()
      .type(SecurityScheme.Type.HTTP)
      .scheme("bearer")
      .bearerFormat("JWT")
      .description("Token de acesso (JWT). Papéis: `operador` (POST /v1/events; RN-13) e "
          + "`operador` / `gerente` (GET; RN-14). Sem token → 401 (E-11); papel não autorizado → 403 (E-12).");

    return new OpenAPI()
      .info(new Info()
        .title("API de Eventos de Fábrica — events-api")
        .version("1.0.0")
        .description("Registro, consulta e estado de saga de eventos de fábrica "
            + "(PRODUCTION, MAINTENANCE, QUALITY). Fonte da verdade do contrato da `events-api` "
            + "(Spring Boot 4 / Java 25). Regra de negócio citada como `RN-xx`: docs/spec/regras-negocio.md. "
            + "Catálogo de erros (type ↔ RN-xx): docs/api/errors.md. Todo erro segue RFC 9457 "
            + "(ProblemDetail): `type`, `title`, `status`, `detail`, `instance`.")
      )
      .servers(List.of(
        new Server().url("https://api.factory/v1").description("Produção"),
        new Server().url("http://localhost:8080/v1").description("Desenvolvimento local")
      ))
      .tags(List.of(
        new Tag().name("eventos").description("Eventos de fábrica: registro idempotente, consulta paginada e obtenção por ID."),
        new Tag().name("sagas").description("Consulta do estado persistido de sagas orquestradas (registro QUALITY → auditoria).")
      ))
      .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth))
      .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
