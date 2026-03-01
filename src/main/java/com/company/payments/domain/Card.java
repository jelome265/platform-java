package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Card entity stores ONLY tokenized references from the BIN sponsor.
 * NEVER stores PAN or CVV — those exist only within the issuing partner's PCI-scoped environment.
 */
@Table("cards")
public class Card {

    @Id
    private UUID id;
    private UUID userId;
    private UUID walletId;
    private String cardToken;       // Token from BIN sponsor (e.g. Marqeta)
    private String last4;           // Last 4 digits for display only
    private String expiryMonth;
    private String expiryYear;
    private String status;          // ACTIVE, FROZEN, CANCELLED
    private String binProfile;      // Card program/profile identifier
    private ZonedDateTime createdAt;

    public Card() {}

    public Card(UUID id, UUID userId, UUID walletId, String cardToken, String last4, String expiryMonth, String expiryYear, String binProfile) {
        this.id = id;
        this.userId = userId;
        this.walletId = walletId;
        this.cardToken = cardToken;
        this.last4 = last4;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.status = "ACTIVE";
        this.binProfile = binProfile;
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getWalletId() { return walletId; }
    public String getCardToken() { return cardToken; }
    public String getLast4() { return last4; }
    public String getExpiryMonth() { return expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getBinProfile() { return binProfile; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
}
