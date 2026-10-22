package com.factory.events.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 2000)
    private String logMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String eventId, String logMessage, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.logMessage = logMessage;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getLogMessage() { return logMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
