package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Table("wallets")
public class Wallet {

    @Id
    private UUID id;
    private UUID userId;
    private String currency;
    private BigDecimal cachedBalance; // Derived from ledger; refreshed periodically
    private ZonedDateTime createdAt;

    public Wallet() {}

    public Wallet(UUID id, UUID userId, String currency) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.cachedBalance = BigDecimal.ZERO;
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCurrency() { return currency; }
    public BigDecimal getCachedBalance() { return cachedBalance; }
    public void setCachedBalance(BigDecimal balance) { this.cachedBalance = balance; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
}
