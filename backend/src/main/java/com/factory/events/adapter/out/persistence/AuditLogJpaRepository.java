package com.factory.events.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository
  extends JpaRepository<AuditLogJpaEntity, Long> {}