package com.company.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("reconciliation_events")
public class ReconciliationEvent {

    @Id
    private UUID id;
    private UUID statementId;
    private String mismatchType; // e.g. "AMOUNT_MISMATCH", "MISSING_LEDGER_ENTRY"
    private String details;
    private ZonedDateTime createdAt;

    public ReconciliationEvent() {}

    public ReconciliationEvent(UUID id, UUID statementId, String mismatchType, String details) {
        this.id = id;
        this.statementId = statementId;
        this.mismatchType = mismatchType;
        this.details = details;
        this.createdAt = ZonedDateTime.now();
    }
}
