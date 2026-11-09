package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.result.IdempotencyOutcome;
import com.factory.events.domain.port.out.IdempotencyStore;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementação JDBC da porta de idempotência.
 * <p>
 * O {@code claim} usa {@code INSERT ... ON CONFLICT DO NOTHING} sobre a
 * UNIQUE(scope, key) — nunca consulta "existe?" antes de inserir.
 */
@Component
public class IdempotencyKeyStore implements IdempotencyStore {

  private static final String SCOPE = "events-api:register";

  private final JdbcTemplate jdbc;

  public IdempotencyKeyStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public IdempotencyOutcome lookup(@NonNull String scope, @NonNull String key) {
    try {
      return jdbc.queryForObject(
        """
          SELECT response_status, response_body, request_hash, expires_at
          FROM idempotency_key
          WHERE scope = ? AND key = ? AND expires_at > now()
          """,
        (rs, rowNum) -> {
          int status = rs.getInt("response_status");
          String body = rs.getString("response_body");
          String hash = rs.getString("request_hash");
          Instant expires = rs.getTimestamp("expires_at").toInstant();
          if (status == 102) {
            return new IdempotencyOutcome.InProgress(expires);
          }
          return new IdempotencyOutcome.Processed(status, body, hash);
        },
        scope, key
      );
    } catch (EmptyResultDataAccessException ex) {
      return new IdempotencyOutcome.None();
    }
  }

  @Override
  public boolean claim(@NonNull String scope, @NonNull String key, @NonNull Instant expiresAt) {
    int inserted = jdbc.update(
      """
        INSERT INTO idempotency_key (scope, key, request_hash, response_status, response_body, expires_at)
        VALUES (?, ?, '', 102, '', ?)
        ON CONFLICT (scope, key) DO NOTHING
        """,
      scope, key, expiresAt
    );
    return inserted == 1;
  }

  @Override
  public void storeResult(@NonNull String scope, @NonNull String key,
                          @NonNull String requestHash, int responseStatus,
                          @NonNull String responseBody, @NonNull Instant expiresAt) {
    jdbc.update(
      """
        UPDATE idempotency_key
        SET request_hash = ?, response_status = ?, response_body = ?, expires_at = ?
        WHERE scope = ? AND key = ?
        """,
      requestHash, responseStatus, responseBody, expiresAt, scope, key
    );
  }

  @Override
  public void purgeExpired(@NonNull Instant before) {
    jdbc.update("DELETE FROM idempotency_key WHERE expires_at < ?", before);
  }
}
