package com.factory.events.domain.model;

import java.time.Instant;

/**
 * Log de auditoria automático para eventos QUALITY. Value object imutável.
 */
public record AuditLog(
  Long id,
  String eventId,
  String logMessage,
  Instant createdAt
) {}
