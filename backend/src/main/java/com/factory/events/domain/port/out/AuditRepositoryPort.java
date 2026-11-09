package com.factory.events.domain.port.out;

import com.factory.events.domain.model.AuditLog;

public interface AuditRepositoryPort {
  void save(AuditLog auditLog);
}