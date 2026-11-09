package com.factory.events.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_event")
public class OutboxEventJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 255)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "event_version", nullable = false, length = 20)
  private String eventVersion;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  public OutboxEventJpaEntity() {}

  public OutboxEventJpaEntity(Long id, String aggregateType, String aggregateId,
                              String eventType, String eventVersion, String payload,
                              Instant occurredAt, Instant publishedAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.eventVersion = eventVersion;
    this.payload = payload;
    this.occurredAt = occurredAt;
    this.publishedAt = publishedAt;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getAggregateType() { return aggregateType; }
  public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
  public String getAggregateId() { return aggregateId; }
  public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getEventVersion() { return eventVersion; }
  public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public Instant getOccurredAt() { return occurredAt; }
  public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
  public Instant getPublishedAt() { return publishedAt; }
  public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
