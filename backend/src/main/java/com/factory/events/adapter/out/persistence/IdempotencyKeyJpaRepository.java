package com.factory.events.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyJpaRepository
  extends JpaRepository<IdempotencyKeyJpaEntity, Long> {

  Optional<IdempotencyKeyJpaEntity> findByScopeAndKey(String scope, String key);

  @Query(
    value = """
      INSERT INTO idempotency_key (scope, key, request_hash, response_status, response_body, expires_at)
      VALUES (:scope, :key, :requestHash, 102, '', :expiresAt)
      ON CONFLICT (scope, key) DO NOTHING
      """,
    nativeQuery = true
  )
  int insertClaim(@Param("scope") String scope,
                  @Param("key") String key,
                  @Param("requestHash") String requestHash,
                  @Param("expiresAt") Instant expiresAt);

  @Query(
    value = """
      UPDATE idempotency_key
      SET request_hash = :requestHash, response_status = :responseStatus,
          response_body = :responseBody, expires_at = :expiresAt
      WHERE scope = :scope AND key = :key
      """,
    nativeQuery = true
  )
  int updateResult(@Param("scope") String scope,
                   @Param("key") String key,
                   @Param("requestHash") String requestHash,
                   @Param("responseStatus") int responseStatus,
                   @Param("responseBody") String responseBody,
                   @Param("expiresAt") Instant expiresAt);
}
