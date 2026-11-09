package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.AuditLog;
import com.factory.events.domain.port.out.AuditRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class AuditRepositoryAdapter implements AuditRepositoryPort {

  private final AuditLogJpaRepository repository;

  public AuditRepositoryAdapter(AuditLogJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(AuditLog auditLog) {
    repository.save(AuditLogPersistenceMapper.toJpa(auditLog));
  }
}