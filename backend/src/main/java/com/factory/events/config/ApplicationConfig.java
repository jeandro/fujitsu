package com.factory.events.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().info(
      new Info()
        .title("Eventos de fábrica")
        .version("1.0.0")
        .description(
          "Cadastro idempotente por eventId e consulta. QUALITY gera auditoria na mesma transação."
        )
    );
  }
}
