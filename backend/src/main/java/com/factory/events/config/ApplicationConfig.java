package com.factory.events.config;

import com.factory.events.domain.port.out.ClockPort;
import com.factory.events.domain.port.out.IdempotencyStore;
import com.factory.events.adapter.in.web.IdempotencyFilter;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * Único lugar com {@link Configuration} e {@link Bean}.
 * Configura clock e registro do filtro de idempotência. O OpenAPI (info/servers/tags/
 * securitySchemes) vive em {@link OpenApiConfig}.
 */
@Configuration
public class ApplicationConfig {

  @Bean
  public ClockPort clockPort() {
    Clock clock = Clock.systemUTC();
    return () -> Instant.now(clock);
  }

  @Bean
  public IdempotencyFilter idempotencyFilterBean(IdempotencyStore idempotencyStore, ObjectMapper objectMapper) {
    return new IdempotencyFilter(idempotencyStore, objectMapper);
  }

  /**
   * Registra o filtro de idempotência apenas para POST /v1/events.
   */
  @Bean
  public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyFilter idempotencyFilterBean) {
    FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(idempotencyFilterBean);
    registration.setUrlPatterns(List.of("/v1/events"));
    registration.setOrder(1);
    registration.setName("idempotencyFilter");
    return registration;
  }
}
