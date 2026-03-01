package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Table("ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;
    private UUID walletId;
    private UUID txId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private ZonedDateTime createdAt;
    private String metadata;

    public LedgerEntry() {}

    public LedgerEntry(UUID id, UUID walletId, UUID txId, String type, BigDecimal amount, String currency, String metadata) {
        this.id = id;
        this.walletId = walletId;
        this.txId = txId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = ZonedDateTime.now();
        this.metadata = metadata;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public UUID getTxId() { return txId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public String getMetadata() { return metadata; }
}
