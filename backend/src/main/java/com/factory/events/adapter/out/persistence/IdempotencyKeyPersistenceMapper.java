package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.result.IdempotencyOutcome;
import java.time.Instant;

public final class IdempotencyKeyPersistenceMapper {

  private IdempotencyKeyPersistenceMapper() {}

  public static IdempotencyKeyJpaEntity toJpa(String scope, String key, String requestHash,
                                              int responseStatus, String responseBody,
                                              Instant expiresAt) {
    return new IdempotencyKeyJpaEntity(
      null, scope, key, requestHash, responseStatus, responseBody, expiresAt, null
    );
  }

  public static IdempotencyOutcome toOutcome(IdempotencyKeyJpaEntity entity) {
    if (entity.getResponseStatus() == 102) {
      return new IdempotencyOutcome.InProgress(entity.getExpiresAt());
    }
    return new IdempotencyOutcome.Processed(
      entity.getResponseStatus(),
      entity.getResponseBody(),
      entity.getRequestHash()
    );
  }
}
