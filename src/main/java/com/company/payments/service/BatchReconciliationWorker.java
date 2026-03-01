package com.company.payments.service;

import com.company.payments.domain.LedgerEntry;
import com.company.payments.repository.LedgerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Batch Reconciliation Worker.
 * Consumes statement lines forwarded by connectors-go and reconciles
 * each against the immutable ledger. Mismatches are surfaced to the
 * compliance review queue.
 */
@Service
public class BatchReconciliationWorker {

    private static final Logger log = LoggerFactory.getLogger(BatchReconciliationWorker.class);
    private final ReconciliationService reconciliationService;
    private final ObjectMapper mapper;

    public BatchReconciliationWorker(ReconciliationService reconciliationService, ObjectMapper mapper) {
        this.reconciliationService = reconciliationService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "reconciliation-statements", groupId = "recon-worker-group")
    public void consumeStatementLine(String message) {
        try {
            JsonNode node = mapper.readTree(message);

            String provider = node.get("provider").asText();
            String providerTxId = node.get("provider_tx_id").asText();
            double amount = node.get("amount").asDouble();
            String currency = node.get("currency").asText();

            UUID statementId = UUID.nameUUIDFromBytes(
                    (provider + ":" + providerTxId).getBytes()
            );

            reconciliationService.reconcileStatementItem(
                    statementId,
                    UUID.fromString(providerTxId),
                    BigDecimal.valueOf(amount),
                    currency
            );

            log.info("[RECON] Processed statement line from {}: tx={}", provider, providerTxId);
        } catch (Exception e) {
            log.error("[RECON] Failed to process statement line", e);
        }
    }
}
