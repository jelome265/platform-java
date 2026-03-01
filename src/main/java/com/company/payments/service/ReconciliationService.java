package com.company.payments.service;

import com.company.payments.domain.LedgerEntry;
import com.company.payments.domain.ReconciliationEvent;
import com.company.payments.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private final LedgerRepository ledgerRepository;

    public ReconciliationService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Reconciles a statement line item against the immutable Ledger.
     * Note: statements are fetched by connectors-go and piped to this service via batch/Kafka.
     */
    public void reconcileStatementItem(UUID statementId, UUID expectedTxId, BigDecimal expectedAmount, String expectedCurrency) {
        log.info("Starting reconciliation for statement {} against TxId {}", statementId, expectedTxId);

        Optional<LedgerEntry> entryOpt = ledgerRepository.findByTxId(expectedTxId);

        if (entryOpt.isEmpty()) {
            log.error("Reconciliation Mismatch: Missing Ledger Entry for statement {}", statementId);
            createReconciliationEvent(statementId, "MISSING_LEDGER_ENTRY", "No ledger found with txId: " + expectedTxId);
            return;
        }

        LedgerEntry entry = entryOpt.get();

        if (entry.getAmount().compareTo(expectedAmount) != 0 || !entry.getCurrency().equals(expectedCurrency)) {
            log.error("Reconciliation Mismatch: Amount or Currency mismatch for txId {}", expectedTxId);
            createReconciliationEvent(
                statementId, 
                "PAYLOAD_MISMATCH", 
                String.format("Expected %s %s but ledger has %s %s", expectedAmount, expectedCurrency, entry.getAmount(), entry.getCurrency())
            );
            return;
        }

        log.info("Reconciliation PASS for statement {}. Identical to Ledger.", statementId);
    }

    private void createReconciliationEvent(UUID statementId, String type, String details) {
        // TODO: Persist event to DB for human review queue / SAR workflow
        ReconciliationEvent event = new ReconciliationEvent(UUID.randomUUID(), statementId, type, details);
        log.warn("Created Reconciliation Event: {}", type);
    }
}
