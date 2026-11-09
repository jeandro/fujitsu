package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.AuditLog;

public final class AuditLogPersistenceMapper {

  private AuditLogPersistenceMapper() {}

  public static AuditLogJpaEntity toJpa(AuditLog domain) {
    return new AuditLogJpaEntity(
      domain.id(),
      domain.eventId(),
      domain.logMessage(),
      domain.createdAt()
    );
  }

  public static AuditLog toDomain(AuditLogJpaEntity entity) {
    return new AuditLog(
      entity.getId(),
      entity.getEventId(),
      entity.getLogMessage(),
      entity.getCreatedAt()
    );
  }
}
