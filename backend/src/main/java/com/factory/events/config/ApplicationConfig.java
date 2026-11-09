package com.factory.events.config;

import com.factory.events.domain.port.out.ClockPort;
import com.factory.events.adapter.in.web.IdempotencyFilter;
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

  /**
   * Registra o filtro de idempotência apenas para POST /v1/events.
   */
  @Bean
  public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(IdempotencyFilter filter) {
    FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.setUrlPatterns(List.of("/v1/events"));
    registration.setOrder(1);
    return registration;
  }
}
