package com.factory.events.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "idempotency_key",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_idempotency_key_scope_key",
    columnNames = {"scope", "key"}
  )
)
public class IdempotencyKeyJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scope", nullable = false, length = 100)
  private String scope;

  @Column(name = "key", nullable = false, length = 255)
  private String key;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "response_status", nullable = false)
  private int responseStatus;

  @Column(name = "response_body", nullable = false, columnDefinition = "text")
  private String responseBody;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public IdempotencyKeyJpaEntity() {}

  public IdempotencyKeyJpaEntity(Long id, String scope, String key, String requestHash,
                                 int responseStatus, String responseBody, Instant expiresAt,
                                 Instant createdAt) {
    this.id = id;
    this.scope = scope;
    this.key = key;
    this.requestHash = requestHash;
    this.responseStatus = responseStatus;
    this.responseBody = responseBody;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public String getKey() { return key; }
  public void setKey(String key) { this.key = key; }
  public String getRequestHash() { return requestHash; }
  public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
  public int getResponseStatus() { return responseStatus; }
  public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }
  public String getResponseBody() { return responseBody; }
  public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
