package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("kyc_verifications")
public class KycVerification {

    @Id
    private UUID id;
    private UUID userId;
    private String providerName;    // e.g. "jumio", "onfido", "trulioo"
    private String providerRefId;   // External reference from ID provider
    private String status;          // PENDING, APPROVED, REJECTED, ESCALATED
    private String resultLevel;     // BASIC, ENHANCED, PROHIBITED
    private String rejectionReason;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public KycVerification() {}

    public KycVerification(UUID id, UUID userId, String providerName) {
        this.id = id;
        this.userId = userId;
        this.providerName = providerName;
        this.status = "PENDING";
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getProviderName() { return providerName; }
    public String getProviderRefId() { return providerRefId; }
    public void setProviderRefId(String ref) { this.providerRefId = ref; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; this.updatedAt = ZonedDateTime.now(); }
    public String getResultLevel() { return resultLevel; }
    public void setResultLevel(String l) { this.resultLevel = l; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }
}
