package com.company.payments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Immutable audit log service. All actions are logged with actor, action, timestamp, and reason.
 * Retention period must comply with RBM and local law (confirm with legal counsel).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void log(UUID actorId, String action, String reason) {
        // In production, persist to an immutable audit_logs table or append to Kafka audit topic
        log.info("[AUDIT] actor={} action={} reason={} timestamp={}", actorId, action, reason, ZonedDateTime.now());
    }
}
