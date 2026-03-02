package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Tracks the multi-step settlement of an FX trade.
 * States: PENDING, LOCKED, COMMITTED, REVERTED, TIMED_OUT.
 */
@Table("settlement_jobs")
public class SettlementJob {

    @Id
    private UUID id;
    private UUID offerId;
    private String status; // PENDING, LOCKED, COMMITTED, REVERTED
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;

    public SettlementJob() {}

    public SettlementJob(UUID id, UUID offerId, int timeoutMinutes) {
        this.id = id;
        this.offerId = offerId;
        this.status = "PENDING";
        this.createdAt = ZonedDateTime.now();
        this.expiresAt = ZonedDateTime.now().plusMinutes(timeoutMinutes);
    }

    public UUID getId() { return id; }
    public UUID getOfferId() { return offerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getExpiresAt() { return expiresAt; }
}
