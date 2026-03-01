package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("aml_alerts")
public class AmlAlert {

    @Id
    private UUID id;
    private UUID userId;
    private String alertType;       // VELOCITY, HIGH_FX, REPEATED_KYC_FAIL, UNUSUAL_DESTINATION
    private String severity;        // LOW, MEDIUM, HIGH, CRITICAL
    private String details;
    private String status;          // OPEN, UNDER_REVIEW, RESOLVED, SAR_FILED
    private ZonedDateTime createdAt;
    private ZonedDateTime resolvedAt;

    public AmlAlert() {}

    public AmlAlert(UUID id, UUID userId, String alertType, String severity, String details) {
        this.id = id;
        this.userId = userId;
        this.alertType = alertType;
        this.severity = severity;
        this.details = details;
        this.status = "OPEN";
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(ZonedDateTime t) { this.resolvedAt = t; }
}
