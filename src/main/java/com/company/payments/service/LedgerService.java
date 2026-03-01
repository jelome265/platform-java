package com.company.payments.service;

import com.company.payments.domain.LedgerEntry;
import com.company.payments.repository.LedgerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private final LedgerRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    public LedgerService(LedgerRepository repository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    /**
     * Strict serializeable insertion to prevent race conditions. 
     * Applies idempotency checks before writing the append-only ledger payload.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void recordEntry(UUID walletId, UUID txId, String type, BigDecimal amount, String currency, Object metadataPayload) {
        // 1. Idempotency Check: deduplicate external webhooks/payments using txId
        Optional<LedgerEntry> existing = repository.findByTxId(txId);
        if (existing.isPresent()) {
            log.warn("Idempotent hit for txId: {}. Skipping duplicate ledger entry.", txId);
            return;
        }

        // 2. Serialize JSONB Metadata
        String metadata;
        try {
            metadata = mapper.writeValueAsString(metadataPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata payload", e);
            throw new RuntimeException("Invalid metadata payload");
        }

        // 3. Create Immutable Ledger Entry
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), walletId, txId, type, amount, currency, metadata);
        repository.save(entry);

        // 4. Send to Kafka (Event Sourcing). Part of same transaction lifecycle
        // Note: For perfect atomicity, use the Outbox Pattern or pg_notify. 
        // Here we rely on Spring Kafka transactional synchronization if configured.
        try {
            String eventPayload = mapper.writeValueAsString(entry);
            kafkaTemplate.send("ledger-events", entry.getWalletId().toString(), eventPayload);
            log.info("Ledger entry inserted and event emitted. txId: {}", txId);
        } catch (Exception e) {
            log.error("Failed to emit kafka event for txId: {}", txId, e);
            throw new RuntimeException("Event publishing failed, rolling back transaction");
        }
    }
}
